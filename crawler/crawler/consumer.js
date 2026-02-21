const http = require('http');
const axios = require('axios');
const {
  QUEUE_JOBS,
  connectRabbit,
  parseMessage,
  publishDlq,
  publishJob
} = require('./lib/rabbitBus');
const { createIdempotencyStore } = require('./lib/idempotencyStore');

const RABBIT_URL = process.env.RABBIT_URL || 'amqp://localhost';
const API_URL = process.env.API_URL || 'http://localhost:8080/api/jobs/import';
const REQUEST_TIMEOUT = Number(process.env.REQUEST_TIMEOUT_MS || 30000);
const API_RETRIES = Number(process.env.API_RETRIES || 3);
const METRICS_PORT = Number(process.env.CONSUMER_METRICS_PORT || 0);

const metrics = {
  received: 0,
  imported: 0,
  retried: 0,
  sentToDlq: 0,
  duplicates: 0,
  invalid: 0,
  processingErrors: 0,
  importedBySource: {},
  retriedBySource: {},
  dlqBySource: {},
  health: 'starting',
  lastProcessedAt: null
};

function esc(value) {
  return String(value)
    .replace(/\\/g, '\\\\')
    .replace(/\n/g, '\\n')
    .replace(/"/g, '\\"');
}

function sourceFromJob(job = {}) {
  return {
    sourceName: job.sourceName || 'unknown',
    sourceType: job.sourceType || 'unknown'
  };
}

function sourceKey(job = {}) {
  const source = sourceFromJob(job);
  return `${source.sourceName}::${source.sourceType}`;
}

function incBySource(map, job) {
  const key = sourceKey(job);
  map[key] = (map[key] || 0) + 1;
}

function asPrometheusMetrics() {
  const lines = [
    '# HELP hermes_consumer_received_total Total de mensagens recebidas para processamento.',
    '# TYPE hermes_consumer_received_total counter',
    `hermes_consumer_received_total ${metrics.received}`,
    '# HELP hermes_consumer_imported_total Total de mensagens importadas com sucesso na API.',
    '# TYPE hermes_consumer_imported_total counter',
    `hermes_consumer_imported_total ${metrics.imported}`,
    '# HELP hermes_consumer_retried_total Total de mensagens reenfileiradas para retry.',
    '# TYPE hermes_consumer_retried_total counter',
    `hermes_consumer_retried_total ${metrics.retried}`,
    '# HELP hermes_consumer_dlq_total Total de mensagens enviadas para DLQ.',
    '# TYPE hermes_consumer_dlq_total counter',
    `hermes_consumer_dlq_total ${metrics.sentToDlq}`,
    '# HELP hermes_consumer_duplicates_total Total de mensagens descartadas por idempotência.',
    '# TYPE hermes_consumer_duplicates_total counter',
    `hermes_consumer_duplicates_total ${metrics.duplicates}`,
    '# HELP hermes_consumer_invalid_total Total de mensagens inválidas descartadas.',
    '# TYPE hermes_consumer_invalid_total counter',
    `hermes_consumer_invalid_total ${metrics.invalid}`,
    '# HELP hermes_consumer_processing_errors_total Total de erros inesperados de processamento.',
    '# TYPE hermes_consumer_processing_errors_total counter',
    `hermes_consumer_processing_errors_total ${metrics.processingErrors}`,
    '# HELP hermes_consumer_up Sinalização de saúde do consumer (1 saudável, 0 degradado).',
    '# TYPE hermes_consumer_up gauge',
    `hermes_consumer_up ${metrics.health === 'healthy' ? 1 : 0}`
  ];

  for (const [key, value] of Object.entries(metrics.importedBySource)) {
    const [sourceName, sourceType] = key.split('::');
    lines.push(`hermes_consumer_imported_by_source_total{source="${esc(sourceName)}",source_type="${esc(sourceType)}"} ${value}`);
  }

  for (const [key, value] of Object.entries(metrics.retriedBySource)) {
    const [sourceName, sourceType] = key.split('::');
    lines.push(`hermes_consumer_retried_by_source_total{source="${esc(sourceName)}",source_type="${esc(sourceType)}"} ${value}`);
  }

  for (const [key, value] of Object.entries(metrics.dlqBySource)) {
    const [sourceName, sourceType] = key.split('::');
    lines.push(`hermes_consumer_dlq_by_source_total{source="${esc(sourceName)}",source_type="${esc(sourceType)}"} ${value}`);
  }

  return `${lines.join('\n')}\n`;
}

function startServer() {
  if (!METRICS_PORT) return;

  http.createServer((req, res) => {
    if (req.url === '/healthz') {
      res.writeHead(metrics.health === 'healthy' ? 200 : 503, { 'content-type': 'application/json' });
      res.end(JSON.stringify(metrics));
      return;
    }

    if (req.url === '/metrics') {
      res.writeHead(200, {
        'content-type': 'text/plain; version=0.0.4; charset=utf-8',
        'cache-control': 'no-store'
      });
      res.end(asPrometheusMetrics());
      return;
    }

    if (req.url === '/metrics/json') {
      res.writeHead(200, { 'content-type': 'application/json' });
      res.end(JSON.stringify(metrics));
      return;
    }

    res.writeHead(404).end();
  }).listen(METRICS_PORT, '0.0.0.0');
}

async function sendJob(job, trace = {}) {
  const headers = {
    'x-ingestion-trace-id': job.ingestionTraceId || 'none'
  };

  if (trace.traceparent) headers.traceparent = trace.traceparent;
  if (trace.tracestate) headers.tracestate = trace.tracestate;

  await axios.post(API_URL, { jobs: [job] }, { timeout: REQUEST_TIMEOUT, headers });
}

async function processMessage(channel, msg, idempotency, send = sendJob) {
  metrics.received += 1;
  metrics.lastProcessedAt = new Date().toISOString();

  const wrapped = parseMessage(msg);
  const job = wrapped?.payload ? wrapped.payload : wrapped;

  if (!job?.url) {
    metrics.invalid += 1;
    channel.ack(msg);
    return;
  }

  const key = `${job.url}::${job.ingestionTraceId || 'none'}`;
  const claimed = await idempotency.claim('consumer', key);
  if (!claimed) {
    metrics.duplicates += 1;
    channel.ack(msg);
    return;
  }

  const retryCount = Number(msg.properties.headers?.retryCount || 0);

  try {
    const trace = {
      traceparent: msg.properties.headers?.traceparent || job.traceparent || null,
      tracestate: msg.properties.headers?.tracestate || job.tracestate || null
    };

    await send(job, trace);
    metrics.imported += 1;
    incBySource(metrics.importedBySource, job);
    channel.ack(msg);
  } catch (error) {
    if (retryCount < API_RETRIES) {
      await publishJob(channel, { ...job, retryCount: retryCount + 1 });
      metrics.retried += 1;
      incBySource(metrics.retriedBySource, job);
      channel.ack(msg);
      return;
    }

    await publishDlq(channel, job, error.message, retryCount);
    metrics.sentToDlq += 1;
    incBySource(metrics.dlqBySource, job);
    channel.ack(msg);
  }
}

async function main() {
  startServer();
  const { channel } = await connectRabbit(RABBIT_URL);
  const idempotency = await createIdempotencyStore();
  channel.prefetch(10);

  await channel.consume(QUEUE_JOBS, msg => {
    processMessage(channel, msg, idempotency).catch(err => {
      metrics.processingErrors += 1;
      metrics.health = 'degraded';
      console.error('consumer error:', err.message);
      if (msg) channel.nack(msg, false, true);
    });
  }, { noAck: false });

  metrics.health = 'healthy';
  console.log('consumer running');
}

if (require.main === module) {
  main();
}

module.exports = { processMessage, sendJob, metrics, asPrometheusMetrics };
