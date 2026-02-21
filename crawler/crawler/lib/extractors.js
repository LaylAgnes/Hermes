const axios = require('axios');

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

function normalizeGreenhouseDetail(item, detail) {
  return {
    url: item.absolute_url,
    title: normalizeText(item.title),
    location: normalizeText(item.location?.name),
    description: normalizeText(detail?.content) || normalizeText(item.title)
  };
}

function normalizeLeverDetail(item) {
  return {
    url: item.hostedUrl,
    title: normalizeText(item.text),
    location: normalizeText(item.categories?.location),
    description: normalizeText(item.descriptionPlain || item.description || item.text)
  };
}

function normalizeScrapedDetail(raw) {
  return {
    url: normalizeText(raw.url),
    title: normalizeText(raw.title),
    location: normalizeText(raw.location),
    description: normalizeText(raw.description)
  };
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

async function withRetries(fn, retries, label) {
  let last;
  for (let i = 1; i <= retries; i++) {
    try {
      return await fn();
    } catch (err) {
      last = err;
      console.warn(`[${label}] tentativa ${i}/${retries} falhou: ${err.message}`);
      if (i < retries) await new Promise(resolve => setTimeout(resolve, 1000 * i));
    }
  }
  throw last;
}

async function extractGreenhouseJobs(source, requestTimeout, maxPerSource) {
  const client = axios.create({ timeout: requestTimeout });
  const token = source.boardToken || source.url.split('/').pop();
  const listApi = `https://boards-api.greenhouse.io/v1/boards/${token}/jobs`;
  const response = await client.get(listApi);
  const list = (response.data.jobs || []).slice(0, maxPerSource);

  const jobs = [];
  for (const item of list) {
    try {
      const detailApi = `https://boards-api.greenhouse.io/v1/boards/${token}/jobs/${item.id}`;
      const detailResp = await client.get(detailApi);
      jobs.push(normalizeGreenhouseDetail(item, detailResp.data || {}));
    } catch {
      jobs.push(normalizeGreenhouseDetail(item, null));
    }
  }

  return jobs;
}

async function extractLeverJobs(source, requestTimeout, maxPerSource) {
  const client = axios.create({ timeout: requestTimeout });
  if (!source.company) throw new Error('company ausente na source lever');
  const api = `https://api.lever.co/v0/postings/${source.company}?mode=json`;
  const response = await client.get(api);
  return (response.data || []).slice(0, maxPerSource).map(normalizeLeverDetail);
}

async function extractWithBrowser(browser, source, listSelector, navTimeout, maxPerSource, detailLocator = '#content, .content, main, article') {
  const page = await browser.newPage();
  page.setDefaultTimeout(navTimeout);
  try {
    await page.goto(source.url, { waitUntil: 'domcontentloaded' });
    await page.waitForSelector(listSelector);

    const links = await page.$$eval(listSelector, (anchors, baseUrl) => [...new Set(anchors.map(a => {
      const href = a.getAttribute('href') || a.href;
      try { return new URL(href, baseUrl).toString(); } catch { return null; }
    }).filter(Boolean))], source.url);

    const jobs = [];
    for (const link of links.slice(0, maxPerSource)) {
      const detail = await browser.newPage();
      detail.setDefaultTimeout(navTimeout);
      try {
        await detail.goto(link, { waitUntil: 'domcontentloaded' });
        const title = await detail.locator('h1,h2').first().textContent().catch(() => null);
        const location = await detail.locator('[class*="location"], [data-qa*="location"], li').first().textContent().catch(() => null);
        const description = await detail.locator(detailLocator).first().innerHTML().catch(() => null);
        jobs.push(normalizeScrapedDetail({ url: link, title, location, description }));
      } finally {
        await detail.close();
      }
    }

    return jobs;
  } finally {
    await page.close();
  }
}

async function collectJobs({ browser, source, sourceRetries, navTimeout, requestTimeout, maxPerSource, parserVersion }) {
  const traceId = `${source.name}-${Date.now()}-${crypto.randomUUID()}`;
  const jobs = await withRetries(async () => {
    if (source.type === 'greenhouse') return extractGreenhouseJobs(source, requestTimeout, maxPerSource);
    if (source.type === 'lever') return extractLeverJobs(source, requestTimeout, maxPerSource);
    if (source.type === 'gupy') return extractWithBrowser(browser, source, 'a[href*="/jobs/"]', navTimeout, maxPerSource);
    if (source.type === 'workday') return extractWithBrowser(browser, source, 'a[href*="/job/"]', navTimeout, maxPerSource);
    throw new Error(`tipo não suportado: ${source.type}`);
  }, sourceRetries, `source-${source.name}`);

  return jobs.map(job => ({
    ...job,
    sourceType: source.type,
    sourceName: source.name,
    parserVersion,
    ingestionTraceId: traceId,
    confidence: confidenceOf(job)
  }));
}

const crypto = require('crypto');

module.exports = {
  validateJob,
  collectJobs
};
