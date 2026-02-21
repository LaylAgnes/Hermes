const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('http');

const { runOnce } = require('../producer');
const { processMessage } = require('../consumer');
const {
  QUEUE_JOBS,
  QUEUE_DLQ,
  connectRabbit,
  parseMessage
} = require('../lib/rabbitBus');

const RABBIT_URL = process.env.RABBIT_URL || 'amqp://localhost';

function createIdempotency() {
  return { async claim() { return true; } };
}

function createJob(trace = 'trace-1') {
  return {
    url: `https://example.com/jobs/${trace}`,
    title: 'Senior Backend Engineer',
    location: 'Remote',
    description: 'Java Spring microservices platform',
    sourceType: 'lever',
    sourceName: 'test-source',
    confidence: 0.9,
    parserVersion: 'v4',
    ingestionTraceId: trace
  };
}

async function startImportServer(statusCode = 200) {
  const imported = [];
  const server = http.createServer((req, res) => {
    if (req.method === 'POST' && req.url === '/api/jobs/import') {
      let body = '';
      req.on('data', c => (body += c.toString('utf8')));
      req.on('end', () => {
        imported.push(JSON.parse(body));
        res.writeHead(statusCode, { 'content-type': 'application/json' });
        res.end(statusCode === 200 ? '1' : '{"error":"boom"}');
      });
      return;
    }
    res.writeHead(404).end();
  });

  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  return {
    imported,
    apiUrl: `http://127.0.0.1:${address.port}/api/jobs/import`,
    close: async () => new Promise(resolve => server.close(resolve))
  };
}

async function waitForMessage(channel, queueName, retries = 30) {
  for (let i = 0; i < retries; i += 1) {
    const msg = await channel.get(queueName, { noAck: false });
    if (msg) return msg;
    await new Promise(resolve => setTimeout(resolve, 50));
  }
  return null;
}

test('real rabbit: producer -> queue -> consumer -> import API', async (t) => {
  let conn;
  let channel;
  try {
    ({ conn, channel } = await connectRabbit(RABBIT_URL));
  } catch (error) {
    t.skip(`RabbitMQ indisponível para teste real: ${error.message}`);
    return;
  }

  await channel.purgeQueue(QUEUE_JOBS);
  await channel.purgeQueue(QUEUE_DLQ);

  const server = await startImportServer(200);

  try {
    const producerIdem = createIdempotency();
    const consumerIdem = createIdempotency();
    const fakeJob = createJob('trace-ok');

    await runOnce(null, channel, producerIdem, {
      sources: [{ name: 'test-source', type: 'lever', url: 'https://example.com' }],
      collector: async () => [fakeJob]
    });

    const msg = await waitForMessage(channel, QUEUE_JOBS);
    assert.ok(msg, 'mensagem não publicada na fila real');

    await processMessage(channel, msg, consumerIdem, async (job) => {
      const payload = JSON.stringify({ jobs: [job] });
      await fetch(server.apiUrl, { method: 'POST', headers: { 'content-type': 'application/json' }, body: payload });
    });

    assert.equal(server.imported.length, 1);
    assert.equal(server.imported[0].jobs[0].url, fakeJob.url);

    const dlqMsg = await channel.get(QUEUE_DLQ, { noAck: true });
    assert.equal(dlqMsg, false);
  } finally {
    await server.close();
    await channel.close();
    await conn.close();
  }
});

test('real rabbit: consumer failure routes message to DLQ', async (t) => {
  let conn;
  let channel;
  try {
    ({ conn, channel } = await connectRabbit(RABBIT_URL));
  } catch (error) {
    t.skip(`RabbitMQ indisponível para teste real: ${error.message}`);
    return;
  }

  await channel.purgeQueue(QUEUE_JOBS);
  await channel.purgeQueue(QUEUE_DLQ);

  const server = await startImportServer(500);

  try {
    const consumerIdem = createIdempotency();
    const fakeJob = createJob('trace-dlq');

    channel.sendToQueue(QUEUE_JOBS, Buffer.from(JSON.stringify(fakeJob)), {
      persistent: true,
      contentType: 'application/json',
      headers: { retryCount: 3 }
    });

    const msg = await waitForMessage(channel, QUEUE_JOBS);
    assert.ok(msg, 'mensagem não encontrada para consumo');

    await processMessage(channel, msg, consumerIdem, async (job) => {
      const payload = JSON.stringify({ jobs: [job] });
      const res = await fetch(server.apiUrl, { method: 'POST', headers: { 'content-type': 'application/json' }, body: payload });
      if (!res.ok) throw new Error(`request failed with status ${res.status}`);
    });

    const dlqMsg = await waitForMessage(channel, QUEUE_DLQ);
    assert.ok(dlqMsg, 'mensagem deveria ter ido para DLQ');

    const dlqEntry = parseMessage(dlqMsg);
    assert.equal(dlqEntry.payload.url, fakeJob.url);
    assert.match(String(dlqEntry.reason), /500/);

    channel.ack(dlqMsg);
  } finally {
    await server.close();
    await channel.close();
    await conn.close();
  }
});
