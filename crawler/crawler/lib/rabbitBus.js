let amqp = null;
try {
  amqp = require('amqplib');
} catch {
  amqp = null;
}

const QUEUE_JOBS = process.env.RABBIT_QUEUE_JOBS || 'hermes.jobs';
const QUEUE_DLQ = process.env.RABBIT_QUEUE_DLQ || 'hermes.jobs.dlq';
const QUEUE_REPLAY = process.env.RABBIT_QUEUE_REPLAY || 'hermes.jobs.replay';

async function connectRabbit(rabbitUrl) {
  if (!amqp) {
    throw new Error('amqplib not installed; run npm install to use RabbitMQ features');
  }

  const conn = await amqp.connect(rabbitUrl);
  const channel = await conn.createChannel();

  await channel.assertQueue(QUEUE_DLQ, { durable: true });
  await channel.assertQueue(QUEUE_REPLAY, { durable: true });
  await channel.assertQueue(QUEUE_JOBS, {
    durable: true,
    deadLetterRoutingKey: QUEUE_DLQ,
    deadLetterExchange: ''
  });

  return { conn, channel };
}

function parseMessage(msg) {
  if (!msg) return null;
  return JSON.parse(msg.content.toString('utf8'));
}

async function publishJob(channel, job) {
  const traceparent = job.traceparent || null;
  const tracestate = job.tracestate || null;

  channel.sendToQueue(QUEUE_JOBS, Buffer.from(JSON.stringify(job)), {
    persistent: true,
    contentType: 'application/json',
    messageId: `${job.url}::${job.ingestionTraceId}`,
    headers: {
      sourceName: job.sourceName,
      sourceType: job.sourceType,
      retryCount: Number(job.retryCount || 0),
      ...(traceparent ? { traceparent } : {}),
      ...(tracestate ? { tracestate } : {})
    }
  });
}

async function publishDlq(channel, payload, reason, retryCount = 0) {
  channel.sendToQueue(QUEUE_DLQ, Buffer.from(JSON.stringify({ payload, reason, retryCount, at: new Date().toISOString() })), {
    persistent: true,
    contentType: 'application/json',
    headers: { reason, retryCount, sourceName: payload?.sourceName || 'unknown' }
  });
}

module.exports = {
  QUEUE_JOBS,
  QUEUE_DLQ,
  QUEUE_REPLAY,
  connectRabbit,
  parseMessage,
  publishJob,
  publishDlq
};
