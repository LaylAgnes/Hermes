const {
  QUEUE_DLQ,
  connectRabbit,
  parseMessage,
  publishJob
} = require('./lib/rabbitBus');

const RABBIT_URL = process.env.RABBIT_URL || 'amqp://localhost';
const SOURCE = process.env.REPLAY_SOURCE || '';
const ERROR_CONTAINS = process.env.REPLAY_ERROR_CONTAINS || '';
const FROM_ISO = process.env.REPLAY_FROM || '';
const TO_ISO = process.env.REPLAY_TO || '';
const LIMIT = Number(process.env.REPLAY_LIMIT || 1000);
const SCAN_LIMIT = Number(process.env.REPLAY_SCAN_LIMIT || 5000);

function inRange(iso) {
  if (!iso) return true;
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return false;
  if (FROM_ISO && t < new Date(FROM_ISO).getTime()) return false;
  if (TO_ISO && t > new Date(TO_ISO).getTime()) return false;
  return true;
}

function matches(entry) {
  if (SOURCE && entry?.payload?.sourceName !== SOURCE) return false;
  if (ERROR_CONTAINS && !String(entry.reason || '').toLowerCase().includes(ERROR_CONTAINS.toLowerCase())) return false;
  return inRange(entry.at);
}

(async () => {
  const { conn, channel } = await connectRabbit(RABBIT_URL);
  let replayed = 0;
  const buffered = [];

  const q = await channel.checkQueue(QUEUE_DLQ);
  const scanCount = Math.min(q.messageCount, SCAN_LIMIT);

  for (let i = 0; i < scanCount; i++) {
    const msg = await channel.get(QUEUE_DLQ, { noAck: false });
    if (!msg) break;

    const entry = parseMessage(msg);
    if (replayed < LIMIT && matches(entry)) {
      await publishJob(channel, entry.payload);
      replayed += 1;
      channel.ack(msg);
      continue;
    }

    buffered.push(msg);
    channel.ack(msg);
  }

  for (const msg of buffered) {
    channel.sendToQueue(QUEUE_DLQ, msg.content, msg.properties);
  }

  console.log(`replayed=${replayed} scanned=${scanCount} kept=${buffered.length}`);
  await channel.close();
  await conn.close();
})();
