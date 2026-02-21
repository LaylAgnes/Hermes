const axios = require('axios');
const {
  QUEUE_JOBS,
  connectRabbit,
  parseMessage,
  publishDlq,
  publishJob
} = require('./lib/rabbitBus');

const RABBIT_URL = process.env.RABBIT_URL || 'amqp://localhost';
const API_URL = process.env.API_URL || 'http://localhost:8080/api/jobs/import';
const REQUEST_TIMEOUT = Number(process.env.REQUEST_TIMEOUT_MS || 30000);
const API_RETRIES = Number(process.env.API_RETRIES || 3);

const seen = new Set();

async function sendJob(job) {
  await axios.post(API_URL, { jobs: [job] }, { timeout: REQUEST_TIMEOUT });
}

async function processMessage(channel, msg) {
  const wrapped = parseMessage(msg);
  const job = wrapped?.payload ? wrapped.payload : wrapped;

  if (!job?.url) {
    channel.ack(msg);
    return;
  }

  const key = `${job.url}::${job.ingestionTraceId || 'none'}`;
  if (seen.has(key)) {
    channel.ack(msg);
    return;
  }

  const retryCount = Number(msg.properties.headers?.retryCount || 0);

  try {
    await sendJob(job);
    seen.add(key);
    channel.ack(msg);
  } catch (error) {
    if (retryCount < API_RETRIES) {
      await publishJob(channel, { ...job, retryCount: retryCount + 1 });
      channel.ack(msg);
      return;
    }

    await publishDlq(channel, job, error.message, retryCount);
    channel.ack(msg);
  }
}

(async () => {
  const { channel } = await connectRabbit(RABBIT_URL);
  channel.prefetch(10);

  await channel.consume(QUEUE_JOBS, msg => {
    processMessage(channel, msg).catch(err => {
      console.error('consumer error:', err.message);
      if (msg) channel.nack(msg, false, true);
    });
  }, { noAck: false });

  console.log('consumer running');
})();
