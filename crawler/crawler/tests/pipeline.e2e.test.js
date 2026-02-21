const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('http');

process.env.API_URL = 'http://127.0.0.1:18080/api/jobs/import';

const { runOnce } = require('../producer');
const { processMessage } = require('../consumer');
const { QUEUE_JOBS, QUEUE_DLQ } = require('../lib/rabbitBus');

function createMemoryChannel() {
  const queues = new Map();

  return {
    queues,
    sendToQueue(name, content, properties = {}) {
      if (!queues.has(name)) queues.set(name, []);
      const msg = { content, properties };
      queues.get(name).push(msg);
      return true;
    },
    ack() {},
    nack() {},
    getQueue(name) {
      return queues.get(name) || [];
    }
  };
}

function createIdempotency() {
  const seen = new Set();
  return {
    async claim(scope, key) {
      const composed = `${scope}:${key}`;
      if (seen.has(composed)) return false;
      seen.add(composed);
      return true;
    }
  };
}

test('crawler producer -> rabbit message -> consumer -> import API', async () => {
  const imported = [];
  const server = http.createServer((req, res) => {
    if (req.method === 'POST' && req.url === '/api/jobs/import') {
      let body = '';
      req.on('data', c => (body += c.toString('utf8')));
      req.on('end', () => {
        imported.push(JSON.parse(body));
        res.writeHead(200, { 'content-type': 'application/json' });
        res.end('1');
      });
      return;
    }
    res.writeHead(404).end();
  });

  await new Promise(resolve => server.listen(18080, '127.0.0.1', resolve));

  const channel = createMemoryChannel();
  const producerIdem = createIdempotency();
  const consumerIdem = createIdempotency();

  const fakeJob = {
    url: 'https://example.com/jobs/1',
    title: 'Senior Backend Engineer',
    location: 'Remote',
    description: 'Java Spring microservices platform',
    sourceType: 'lever',
    sourceName: 'test-source',
    confidence: 0.9,
    parserVersion: 'v4',
    ingestionTraceId: 'trace-1'
  };

  await runOnce(null, channel, producerIdem, {
    sources: [{ name: 'test-source', type: 'lever', url: 'https://example.com' }],
    collector: async () => [fakeJob]
  });

  const queued = channel.getQueue(QUEUE_JOBS);
  assert.equal(queued.length, 1);

  await processMessage(channel, queued[0], consumerIdem);

  assert.equal(imported.length, 1);
  assert.equal(imported[0].jobs[0].url, fakeJob.url);
  assert.equal(channel.getQueue(QUEUE_DLQ).length, 0);

  server.close();
});
