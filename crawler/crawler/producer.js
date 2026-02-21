const http = require('http');
const { chromium } = require('playwright');
const sources = require('./sources');
const { collectJobs, validateJob } = require('./lib/extractors');
const { connectRabbit, publishJob } = require('./lib/rabbitBus');
const { createIdempotencyStore } = require('./lib/idempotencyStore');

const RABBIT_URL = process.env.RABBIT_URL || 'amqp://localhost';
const HEADLESS = process.env.HEADLESS !== 'false';
const RUN_MODE = process.env.RUN_MODE || 'batch';
const POLL_INTERVAL_MS = Number(process.env.POLL_INTERVAL_MS || 15 * 60 * 1000);
const MAX_JOBS_PER_SOURCE = Number(process.env.MAX_JOBS_PER_SOURCE || 30);
const NAV_TIMEOUT = Number(process.env.NAV_TIMEOUT_MS || 45000);
const REQUEST_TIMEOUT = Number(process.env.REQUEST_TIMEOUT_MS || 30000);
const SOURCE_RETRIES = Number(process.env.MAX_SOURCE_RETRIES || 2);
const METRICS_PORT = Number(process.env.METRICS_PORT || 0);
const PARSER_VERSION = process.env.PARSER_VERSION || 'v4';

const metrics = {
  runs: 0,
  published: 0,
  dropped: 0,
  sourceFailures: 0,
  health: 'starting',
  lastRunAt: null
};

function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

function startServer() {
  if (!METRICS_PORT) return;
  http.createServer((req, res) => {
    if (req.url === '/healthz') {
      res.writeHead(metrics.health === 'healthy' ? 200 : 503, { 'content-type': 'application/json' });
      res.end(JSON.stringify(metrics));
      return;
    }
    if (req.url === '/metrics') {
      res.writeHead(200, { 'content-type': 'application/json' });
      res.end(JSON.stringify(metrics));
      return;
    }
    res.writeHead(404).end();
  }).listen(METRICS_PORT, '0.0.0.0');
}

async function runOnce(browser, channel, idempotency, opts = {}) {
  const collector = opts.collector || collectJobs;
  const sourceList = opts.sources || sources;

  metrics.runs += 1;
  metrics.lastRunAt = new Date().toISOString();

  for (const source of sourceList) {
    try {
      const jobs = await collector({
        browser,
        source,
        sourceRetries: SOURCE_RETRIES,
        navTimeout: NAV_TIMEOUT,
        requestTimeout: REQUEST_TIMEOUT,
        maxPerSource: MAX_JOBS_PER_SOURCE,
        parserVersion: PARSER_VERSION
      });

      for (const job of jobs) {
        const key = `${job.url}::${job.ingestionTraceId}`;
        const claimed = await idempotency.claim('producer', key);
        if (!claimed) continue;

        const result = validateJob(job);
        if (!result.valid) {
          metrics.dropped += 1;
          continue;
        }
        await publishJob(channel, result.job);
        metrics.published += 1;
      }
    } catch (error) {
      metrics.sourceFailures += 1;
      console.error(`[producer:${source.name}] ${error.message}`);
    }
  }

  metrics.health = metrics.sourceFailures > 0 ? 'degraded' : 'healthy';
}

async function main() {
  startServer();
  const { conn, channel } = await connectRabbit(RABBIT_URL);
  const browser = await chromium.launch({ headless: HEADLESS });
  const idempotency = await createIdempotencyStore();

  try {
    if (RUN_MODE === 'continuous') {
      while (true) {
        await runOnce(browser, channel, idempotency);
        await sleep(POLL_INTERVAL_MS);
      }
    } else {
      await runOnce(browser, channel, idempotency);
    }
  } finally {
    if (RUN_MODE !== 'continuous') {
      await browser.close();
      await idempotency.close();
      await channel.close();
      await conn.close();
    }
  }
}

if (require.main === module) {
  main();
}

module.exports = { runOnce, metrics };
