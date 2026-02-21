const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');
const axios = require('axios');
const sources = require('./sources');

const API_URL = process.env.API_URL || 'http://localhost:8080/api/jobs/import';
const HEADLESS = process.env.HEADLESS !== 'false';
const MAX_JOBS_PER_SOURCE = Number(process.env.MAX_JOBS_PER_SOURCE || 30);
const NAV_TIMEOUT = Number(process.env.NAV_TIMEOUT_MS || 45000);
const REQUEST_TIMEOUT = Number(process.env.REQUEST_TIMEOUT_MS || 30000);
const RETRIES = Number(process.env.API_RETRIES || 3);
const RUN_MODE = process.env.RUN_MODE || 'batch'; // batch | continuous
const POLL_INTERVAL_MS = Number(process.env.POLL_INTERVAL_MS || 15 * 60 * 1000);
const MAX_SOURCE_RETRIES = Number(process.env.MAX_SOURCE_RETRIES || 2);
const DLQ_PATH = process.env.DLQ_PATH || path.join(__dirname, 'dlq.jsonl');

const metrics = {
  runs: 0,
  jobsCollected: 0,
  jobsValidated: 0,
  jobsDropped: 0,
  jobsSent: 0,
  sourceFailures: 0,
  apiFailures: 0,
  lastRunAt: null,
  lastDurationMs: 0,
  healthStatus: 'starting'
};

function nowIso() {
  return new Date().toISOString();
}

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function withRetries(fn, label, retries = RETRIES) {
  let lastError;

  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;
      console.warn(`[${label}] tentativa ${attempt}/${retries} falhou: ${error.message}`);
      if (attempt < retries) await sleep(1000 * attempt);
    }
  }

  throw lastError;
}

function normalizeText(value) {
  if (!value) return null;
  const normalized = String(value).replace(/\s+/g, ' ').trim();
  return normalized || null;
}

function absoluteUrl(baseUrl, href) {
  try {
    return new URL(href, baseUrl).toString();
  } catch {
    return null;
  }
}

function sanitizeDescription(description) {
  return normalizeText(description)?.slice(0, 8000) || null;
}

function normalizeLocation(location) {
  return normalizeText(location);
}

function computeConfidence(job) {
  let score = 0;
  if (job.url) score += 0.4;
  if (job.title) score += 0.25;
  if (job.location) score += 0.15;
  if (job.description && job.description.length >= 120) score += 0.2;
  return Number(Math.min(score, 1).toFixed(2));
}

function validateJob(job, source) {
  const normalized = {
    url: normalizeText(job.url),
    title: normalizeText(job.title),
    location: normalizeLocation(job.location),
    description: sanitizeDescription(job.description)
  };

  const errors = [];
  if (!normalized.url) errors.push('url ausente');
  if (!normalized.title) errors.push('title ausente');
  if (!normalized.description) errors.push('description ausente');

  return {
    valid: errors.length === 0,
    errors,
    source: source.name,
    confidence: computeConfidence(normalized),
    job: normalized
  };
}

async function appendDlq(entry) {
  await fs.promises.mkdir(path.dirname(DLQ_PATH), { recursive: true });
  await fs.promises.appendFile(DLQ_PATH, `${JSON.stringify({ timestamp: nowIso(), ...entry })}\n`, 'utf8');
}

async function extractGreenhouseJobs(browser, source) {
  const page = await browser.newPage();
  page.setDefaultTimeout(NAV_TIMEOUT);

  try {
    await page.goto(source.url, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('a[href*="/jobs/"]');

    const links = await page.$$eval('a[href*="/jobs/"]', anchors =>
      [...new Set(anchors.map(a => a.href).filter(Boolean))]
    );

    return extractFromDetailPages(browser, source, links);
  } finally {
    await page.close();
  }
}

async function extractLeverJobs(browser, source) {
  const page = await browser.newPage();
  page.setDefaultTimeout(NAV_TIMEOUT);

  try {
    await page.goto(source.url, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('a[href*="/jobs/"]');

    const links = await page.$$eval('a[href*="/jobs/"]', anchors =>
      [...new Set(anchors.map(a => a.href).filter(Boolean))]
    );

    return extractFromDetailPages(browser, source, links);
  } finally {
    await page.close();
  }
}

async function extractWorkdayJobs(browser, source) {
  const page = await browser.newPage();
  page.setDefaultTimeout(NAV_TIMEOUT);

  try {
    await page.goto(source.url, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1500);

    const links = await page.$$eval('a[href*="/job/"]', (anchors, baseUrl) =>
      [...new Set(anchors.map(a => {
        try { return new URL(a.getAttribute('href'), baseUrl).toString(); } catch { return null; }
      }).filter(Boolean))],
      source.url
    );

    return extractFromDetailPages(browser, source, links);
  } finally {
    await page.close();
  }
}

async function extractGupyJobs(browser, source) {
  const page = await browser.newPage();
  page.setDefaultTimeout(NAV_TIMEOUT);

  try {
    await page.goto(source.url, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector('a[href*="/jobs/"]');

    const links = await page.$$eval('a[href*="/jobs/"]', (anchors, baseUrl) =>
      [...new Set(anchors.map(a => {
        const href = a.getAttribute('href');
        try { return new URL(href, baseUrl).toString(); } catch { return null; }
      }).filter(Boolean))],
      source.url
    );

    return extractFromDetailPages(browser, source, links);
  } finally {
    await page.close();
  }
}

async function extractFromDetailPages(browser, source, links) {
  const selectedLinks = links.slice(0, MAX_JOBS_PER_SOURCE);
  const jobs = [];

  for (const link of selectedLinks) {
    const jobPage = await browser.newPage();
    jobPage.setDefaultTimeout(NAV_TIMEOUT);

    try {
      await jobPage.goto(link, { waitUntil: 'domcontentloaded' });

      const title = normalizeText(await jobPage.locator('h1, h2').first().textContent().catch(() => null));
      const location = normalizeText(await jobPage.locator('[class*="location"], [data-qa*="location"], li').first().textContent().catch(() => null));
      const description = normalizeText(await jobPage.locator('#content, .content, main, article, [class*="description"]').first().textContent().catch(() => null));

      jobs.push({ url: absoluteUrl(source.url, link), title, location, description });
    } catch (error) {
      console.warn(`[${source.name}] falha ao processar job ${link}: ${error.message}`);
      await appendDlq({ stage: 'extract-job', source: source.name, sourceType: source.type, url: link, reason: error.message });
    } finally {
      await jobPage.close();
    }
  }

  return jobs;
}

const extractors = {
  greenhouse: extractGreenhouseJobs,
  lever: extractLeverJobs,
  workday: extractWorkdayJobs,
  gupy: extractGupyJobs
};

async function collectJobsFromSource(browser, source) {
  const extractor = extractors[source.type];
  if (!extractor) {
    console.warn(`[${source.name}] tipo de fonte não suportado: ${source.type}`);
    await appendDlq({ stage: 'extract-source', source: source.name, sourceType: source.type, reason: 'source type not supported' });
    return [];
  }

  return withRetries(async () => {
    const jobs = await extractor(browser, source);
    return jobs;
  }, `source-${source.name}`, MAX_SOURCE_RETRIES);
}

async function sendToApi(jobs) {
  if (!jobs.length) {
    console.log('Nenhuma vaga coletada para envio.');
    return;
  }

  await withRetries(
    () => axios.post(API_URL, { jobs }, { timeout: REQUEST_TIMEOUT }),
    'envio-api'
  );

  metrics.jobsSent += jobs.length;
  console.log(`Enviado para API: ${jobs.length} vagas`);
}

async function runOnce(browser) {
  const startedAt = Date.now();
  metrics.runs += 1;
  metrics.lastRunAt = nowIso();

  const allJobs = [];

  for (const source of sources) {
    console.log(`\n[${source.name}] coletando em ${source.url}`);
    try {
      const jobs = await collectJobsFromSource(browser, source);
      metrics.jobsCollected += jobs.length;
      console.log(`[${source.name}] vagas encontradas: ${jobs.length}`);
      allJobs.push(...jobs);
    } catch (error) {
      metrics.sourceFailures += 1;
      console.error(`[${source.name}] falha: ${error.message}`);
      await appendDlq({ stage: 'extract-source', source: source.name, sourceType: source.type, reason: error.message });
    }
  }

  const validated = [];
  for (const job of allJobs) {
    const check = validateJob(job, { name: 'mixed' });
    if (!check.valid) {
      metrics.jobsDropped += 1;
      await appendDlq({ stage: 'validation', source: check.source, url: check.job.url, reason: check.errors.join(', '), job: check.job });
      continue;
    }
    validated.push(check.job);
  }

  metrics.jobsValidated += validated.length;

  const uniqueJobs = Object.values(
    validated.reduce((acc, job) => {
      if (job?.url) acc[job.url] = job;
      return acc;
    }, {})
  );

  console.log(`\nTotal coletado (sem duplicatas): ${uniqueJobs.length}`);

  try {
    await sendToApi(uniqueJobs);
  } catch (error) {
    metrics.apiFailures += 1;
    await appendDlq({ stage: 'api', reason: error.message, count: uniqueJobs.length });
    throw error;
  } finally {
    metrics.lastDurationMs = Date.now() - startedAt;
    metrics.healthStatus = metrics.apiFailures > 0 || metrics.sourceFailures > 0 ? 'degraded' : 'healthy';
    console.log(`[metrics] ${JSON.stringify(metrics)}`);
  }
}

(async () => {
  const browser = await chromium.launch({ headless: HEADLESS });

  try {
    if (RUN_MODE === 'continuous') {
      console.log(`Crawler em modo contínuo (intervalo: ${POLL_INTERVAL_MS}ms)`);
      while (true) {
        try {
          await runOnce(browser);
        } catch (error) {
          console.error('Execução contínua com falha:', error.message);
          metrics.healthStatus = 'degraded';
        }

        await sleep(POLL_INTERVAL_MS);
      }
    } else {
      await runOnce(browser);
    }
  } catch (error) {
    console.error('Crawler falhou:', error.message);
    process.exitCode = 1;
  } finally {
    if (RUN_MODE !== 'continuous') await browser.close();
  }
})();
