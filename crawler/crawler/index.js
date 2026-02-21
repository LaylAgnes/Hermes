const { chromium } = require('playwright');
const axios = require('axios');
const sources = require('./sources');

const API_URL = process.env.API_URL || 'http://localhost:8080/api/jobs/import';
const HEADLESS = process.env.HEADLESS !== 'false';
const MAX_JOBS_PER_SOURCE = Number(process.env.MAX_JOBS_PER_SOURCE || 30);
const NAV_TIMEOUT = Number(process.env.NAV_TIMEOUT_MS || 45000);
const REQUEST_TIMEOUT = Number(process.env.REQUEST_TIMEOUT_MS || 30000);
const RETRIES = Number(process.env.API_RETRIES || 3);

async function withRetries(fn, label) {
  let lastError;

  for (let attempt = 1; attempt <= RETRIES; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;
      console.warn(`[${label}] tentativa ${attempt}/${RETRIES} falhou: ${error.message}`);
      if (attempt < RETRIES) {
        await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
      }
    }
  }

  throw lastError;
}

function normalizeText(value) {
  if (!value) return null;
  const normalized = String(value).replace(/\s+/g, ' ').trim();
  return normalized || null;
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

    const selectedLinks = links.slice(0, MAX_JOBS_PER_SOURCE);
    const jobs = [];

    for (const link of selectedLinks) {
      const jobPage = await browser.newPage();
      jobPage.setDefaultTimeout(NAV_TIMEOUT);

      try {
        await jobPage.goto(link, { waitUntil: 'domcontentloaded' });

        const title = normalizeText(await jobPage.locator('h1').first().textContent().catch(() => null));
        const location = normalizeText(await jobPage.locator('[class*="location"]').first().textContent().catch(() => null));
        const description = normalizeText(await jobPage.locator('#content, .content, main').first().textContent().catch(() => null));

        jobs.push({
          url: link,
          title,
          location,
          description
        });
      } catch (error) {
        console.warn(`[${source.name}] falha ao processar job ${link}: ${error.message}`);
      } finally {
        await jobPage.close();
      }
    }

    return jobs;
  } finally {
    await page.close();
  }
}

async function collectJobsFromSource(browser, source) {
  if (source.type === 'greenhouse') {
    return extractGreenhouseJobs(browser, source);
  }

  console.warn(`[${source.name}] tipo de fonte não suportado: ${source.type}`);
  return [];
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

  console.log(`Enviado para API: ${jobs.length} vagas`);
}

(async () => {
  const browser = await chromium.launch({ headless: HEADLESS });

  try {
    const allJobs = [];

    for (const source of sources) {
      console.log(`\n[${source.name}] coletando em ${source.url}`);
      const jobs = await collectJobsFromSource(browser, source);
      console.log(`[${source.name}] vagas encontradas: ${jobs.length}`);
      allJobs.push(...jobs);
    }

    const uniqueJobs = Object.values(
      allJobs.reduce((acc, job) => {
        if (job?.url) acc[job.url] = job;
        return acc;
      }, {})
    );

    console.log(`\nTotal coletado (sem duplicatas): ${uniqueJobs.length}`);

    await sendToApi(uniqueJobs);
  } catch (error) {
    console.error('Crawler falhou:', error.message);
    process.exitCode = 1;
  } finally {
    await browser.close();
  }
})();
