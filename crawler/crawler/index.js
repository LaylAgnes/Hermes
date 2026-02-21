const fs = require('fs');
const path = require('path');
const http = require('http');
const crypto = require('crypto');
const axios = require('axios');
const { chromium } = require('playwright');
const sources = require('./sources');

const API_URL = process.env.API_URL || 'http://localhost:8080/api/jobs/import';
const HEADLESS = process.env.HEADLESS !== 'false';
const RUN_MODE = process.env.RUN_MODE || 'batch';
const POLL_INTERVAL_MS = Number(process.env.POLL_INTERVAL_MS || 15 * 60 * 1000);
const MAX_JOBS_PER_SOURCE = Number(process.env.MAX_JOBS_PER_SOURCE || 30);
const NAV_TIMEOUT = Number(process.env.NAV_TIMEOUT_MS || 45000);
const REQUEST_TIMEOUT = Number(process.env.REQUEST_TIMEOUT_MS || 30000);
const API_RETRIES = Number(process.env.API_RETRIES || 3);
const SOURCE_RETRIES = Number(process.env.MAX_SOURCE_RETRIES || 2);
const DLQ_PATH = process.env.DLQ_PATH || path.join(__dirname, 'dlq.jsonl');
const QUEUE_PATH = process.env.QUEUE_PATH || path.join(__dirname, 'queue.jsonl');
const BATCH_SIZE = Number(process.env.BATCH_SIZE || 50);
const METRICS_PORT = Number(process.env.METRICS_PORT || 0);
const PARSER_VERSION = process.env.PARSER_VERSION || 'v3';

const metrics = {
  startedAt: new Date().toISOString(),
  runs: 0,
  sourceSuccess: 0,
  sourceFailures: 0,
  queuedJobs: 0,
  sentJobs: 0,
  droppedJobs: 0,
  apiFailures: 0,
  dlqWrites: 0,
  dlqReplayed: 0,
  queueRecovered: 0,
  sourceStats: {},
  lastRunAt: null,
  lastRunDurationMs: 0,
  healthStatus: 'starting'
};

const queue = [];
const seenUrls = new Set();

const greenhouseClient = axios.create({ timeout: REQUEST_TIMEOUT });
const leverClient = axios.create({ timeout: REQUEST_TIMEOUT });

function nowIso() { return new Date().toISOString(); }
function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }

async function withRetries(fn, retries, label) {
  let last;
  for (let i = 1; i <= retries; i++) {
    try {
      return await fn();
    } catch (err) {
      last = err;
      console.warn(`[${label}] tentativa ${i}/${retries} falhou: ${err.message}`);
      if (i < retries) await sleep(i * 1000);
    }
  }
  throw last;
}

function normalizeText(value) {
  if (!value) return null;
  const text = String(value).replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim();
  return text || null;
}

function confidenceOf(job) {
  let score = 0;
  if (job.url) score += 0.35;
  if (job.title) score += 0.25;
  if (job.description && job.description.length > 220) score += 0.25;
  if (job.location) score += 0.15;
  return Number(Math.min(score, 1).toFixed(2));
}

function validateJob(job) {
  const cleaned = {
    url: normalizeText(job.url),
    title: normalizeText(job.title),
    location: normalizeText(job.location),
    description: normalizeText(job.description)?.slice(0, 8000) || null,
    sourceType: normalizeText(job.sourceType),
    sourceName: normalizeText(job.sourceName),
    confidence: Number(job.confidence || 0),
    parserVersion: normalizeText(job.parserVersion),
    ingestionTraceId: normalizeText(job.ingestionTraceId)
  };

  const errors = [];
  if (!cleaned.url) errors.push('url ausente');
  if (!cleaned.title) errors.push('title ausente');
  if (!cleaned.description) errors.push('description ausente');
  if (!cleaned.sourceType) errors.push('sourceType ausente');

  return { valid: errors.length === 0, errors, job: cleaned };
}

async function appendJsonl(filePath, entry) {
  await fs.promises.mkdir(path.dirname(filePath), { recursive: true });
  await fs.promises.appendFile(filePath, `${JSON.stringify(entry)}\n`, 'utf8');
}

async function appendDlq(entry) {
  await appendJsonl(DLQ_PATH, { timestamp: nowIso(), ...entry });
  metrics.dlqWrites += 1;
}

function createTraceId(source) {
  return `${source.name}-${Date.now()}-${crypto.randomUUID()}`;
}

function mapJob(source, raw, traceId) {
  const job = {
    url: raw.url,
    title: raw.title,
    location: raw.location,
    description: raw.description,
    sourceType: source.type,
    sourceName: source.name,
    parserVersion: PARSER_VERSION,
    ingestionTraceId: traceId
  };
  job.confidence = confidenceOf(job);
  return job;
}

async function extractGreenhouseJobs(source) {
  const token = source.boardToken || source.url.split('/').pop();
  const listApi = `https://boards-api.greenhouse.io/v1/boards/${token}/jobs`;
  const response = await greenhouseClient.get(listApi);
  const list = (response.data.jobs || []).slice(0, MAX_JOBS_PER_SOURCE);

  const jobs = [];
  for (const item of list) {
    try {
      const detailApi = `https://boards-api.greenhouse.io/v1/boards/${token}/jobs/${item.id}`;
      const detailResp = await greenhouseClient.get(detailApi);
      const detail = detailResp.data || {};
      jobs.push({
        url: item.absolute_url,
        title: item.title,
        location: item.location?.name || null,
        description: detail.content || detail.metadata?.find(x => x.name === 'Descrição')?.value || item.title
      });
    } catch {
      jobs.push({
        url: item.absolute_url,
        title: item.title,
        location: item.location?.name || null,
        description: item.title
      });
    }
  }

  return jobs;
}

async function extractLeverJobs(source) {
  if (!source.company) {
    throw new Error('company ausente na source lever');
  }
  const api = `https://api.lever.co/v0/postings/${source.company}?mode=json`;
  const response = await leverClient.get(api);
  return (response.data || []).slice(0, MAX_JOBS_PER_SOURCE).map(item => ({
    url: item.hostedUrl,
    title: item.text,
    location: item.categories?.location || null,
    description: item.descriptionPlain || item.description || item.text
  }));
}

async function extractWithBrowser(browser, source, listSelector, detailLocator = '#content, .content, main, article') {
  const page = await browser.newPage();
  page.setDefaultTimeout(NAV_TIMEOUT);
  try {
    await page.goto(source.url, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector(listSelector);

    const links = await page.$$eval(listSelector, (anchors, baseUrl) => {
      return [...new Set(anchors.map(a => {
        const href = a.getAttribute('href') || a.href;
        try { return new URL(href, baseUrl).toString(); } catch { return null; }
      }).filter(Boolean))];
    }, source.url);

    const jobs = [];
    for (const link of links.slice(0, MAX_JOBS_PER_SOURCE)) {
      const detail = await browser.newPage();
      detail.setDefaultTimeout(NAV_TIMEOUT);
      try {
        await detail.goto(link, { waitUntil: 'domcontentloaded' });
        const title = normalizeText(await detail.locator('h1,h2').first().textContent().catch(() => null));
        const location = normalizeText(await detail.locator('[class*="location"], [data-qa*="location"], li').first().textContent().catch(() => null));
        const description = normalizeText(await detail.locator(detailLocator).first().innerHTML().catch(() => null));
        jobs.push({ url: link, title, location, description });
      } finally {
        await detail.close();
      }
    }
    return jobs;
  } finally {
    await page.close();
  }
}

const extractors = {
  greenhouse: async (browser, source) => extractGreenhouseJobs(source),
  lever: async (browser, source) => extractLeverJobs(source),
  gupy: async (browser, source) => extractWithBrowser(browser, source, 'a[href*="/jobs/"]'),
  workday: async (browser, source) => extractWithBrowser(browser, source, 'a[href*="/job/"]')
};

async function collectSource(browser, source) {
  const extractor = extractors[source.type];
  if (!extractor) throw new Error(`tipo não suportado: ${source.type}`);

  const traceId = createTraceId(source);

  return withRetries(async () => {
    const rawJobs = await extractor(browser, source);
    return rawJobs.map(job => mapJob(source, job, traceId));
  }, SOURCE_RETRIES, `source-${source.name}`);
}

async function recoverQueueFromDisk() {
  if (!fs.existsSync(QUEUE_PATH)) return;
  const lines = (await fs.promises.readFile(QUEUE_PATH, 'utf8')).split('\n').filter(Boolean);
  for (const line of lines) {
    try {
      const job = JSON.parse(line);
      queue.push(job);
      seenUrls.add(job.url);
      metrics.queueRecovered += 1;
    } catch {
      // ignore corrupted line
    }
  }
}

async function persistQueue() {
  const content = queue.map(item => JSON.stringify(item)).join('\n');
  await fs.promises.mkdir(path.dirname(QUEUE_PATH), { recursive: true });
  await fs.promises.writeFile(QUEUE_PATH, content + (content ? '\n' : ''), 'utf8');
}

async function enqueueJobs(jobs) {
  for (const job of jobs) {
    if (!job.url || seenUrls.has(job.url)) continue;
    seenUrls.add(job.url);

    const validation = validateJob(job);
    if (!validation.valid) {
      metrics.droppedJobs += 1;
      await appendDlq({ stage: 'validation', url: job.url, reason: validation.errors.join(', '), job: validation.job });
      continue;
    }

    queue.push(validation.job);
    metrics.queuedJobs += 1;
  }

  await persistQueue();
}

async function sendBatch(batch) {
  await withRetries(() => axios.post(API_URL, { jobs: batch }, { timeout: REQUEST_TIMEOUT }), API_RETRIES, 'api');
  metrics.sentJobs += batch.length;
}

async function flushQueue() {
  while (queue.length > 0) {
    const batch = queue.slice(0, BATCH_SIZE);
    try {
      await sendBatch(batch);
      queue.splice(0, batch.length);
      await persistQueue();
    } catch (err) {
      metrics.apiFailures += 1;
      await appendDlq({ stage: 'api', reason: err.message, batch });
      break;
    }
  }
}

async function replayDlqApi() {
  if (!fs.existsSync(DLQ_PATH)) return;
  const lines = (await fs.promises.readFile(DLQ_PATH, 'utf8')).split('\n').filter(Boolean);
  const keep = [];

  for (const line of lines) {
    try {
      const entry = JSON.parse(line);
      if (entry.stage !== 'api' || !Array.isArray(entry.batch) || !entry.batch.length) {
        keep.push(line);
        continue;
      }

      await sendBatch(entry.batch);
      metrics.dlqReplayed += entry.batch.length;
    } catch {
      keep.push(line);
    }
  }

  await fs.promises.writeFile(DLQ_PATH, keep.join('\n') + (keep.length ? '\n' : ''), 'utf8');
}

function startMetricsServer() {
  if (!METRICS_PORT) return;
  const server = http.createServer((req, res) => {
    if (req.url === '/healthz') {
      res.writeHead(metrics.healthStatus === 'healthy' ? 200 : 503, { 'content-type': 'application/json' });
      res.end(JSON.stringify({ status: metrics.healthStatus, lastRunAt: metrics.lastRunAt }));
      return;
    }

    if (req.url === '/metrics') {
      res.writeHead(200, { 'content-type': 'application/json' });
      res.end(JSON.stringify(metrics));
      return;
    }

    res.writeHead(404);
    res.end('not found');
  });

  server.listen(METRICS_PORT, '0.0.0.0', () => {
    console.log(`metrics server listening at :${METRICS_PORT}`);
  });
}

function ensureSourceStat(sourceName) {
  if (!metrics.sourceStats[sourceName]) {
    metrics.sourceStats[sourceName] = { successRuns: 0, failRuns: 0, lastCount: 0, lastError: null };
  }
  return metrics.sourceStats[sourceName];
}

async function runOnce(browser) {
  const start = Date.now();
  metrics.runs += 1;
  metrics.lastRunAt = nowIso();

  for (const source of sources) {
    console.log(`\n[${source.name}] iniciando coleta (${source.type})`);
    const sourceStat = ensureSourceStat(source.name);
    try {
      const jobs = await collectSource(browser, source);
      await enqueueJobs(jobs);
      metrics.sourceSuccess += 1;
      sourceStat.successRuns += 1;
      sourceStat.lastCount = jobs.length;
      sourceStat.lastError = null;
      console.log(`[${source.name}] coletadas: ${jobs.length}`);
    } catch (err) {
      metrics.sourceFailures += 1;
      sourceStat.failRuns += 1;
      sourceStat.lastError = err.message;
      await appendDlq({ stage: 'extract-source', source: source.name, sourceType: source.type, reason: err.message });
      console.error(`[${source.name}] erro: ${err.message}`);
    }
  }

  await replayDlqApi();
  await flushQueue();

  metrics.lastRunDurationMs = Date.now() - start;
  metrics.healthStatus = metrics.apiFailures > 0 || metrics.sourceFailures > 0 ? 'degraded' : 'healthy';
  console.log(`[metrics] ${JSON.stringify(metrics)}`);
}

(async () => {
  startMetricsServer();
  await recoverQueueFromDisk();
  const browser = await chromium.launch({ headless: HEADLESS });

  try {
    if (RUN_MODE === 'continuous') {
      while (true) {
        await runOnce(browser);
        await sleep(POLL_INTERVAL_MS);
      }
    } else {
      await runOnce(browser);
    }
  } catch (err) {
    metrics.healthStatus = 'unhealthy';
    console.error('crawler fatal:', err.message);
    process.exitCode = 1;
  } finally {
    if (RUN_MODE !== 'continuous') await browser.close();
  }
})();
