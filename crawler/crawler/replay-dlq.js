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
  let handled = 0;

  while (handled < LIMIT) {
    const msg = await channel.get(QUEUE_DLQ, { noAck: false });
    if (!msg) break;

    const entry = parseMessage(msg);
    if (matches(entry)) {
      await publishJob(channel, entry.payload);
      handled += 1;
      channel.ack(msg);
      continue;
    }

    channel.nack(msg, false, true);
    break;
  }

  console.log(`replayed=${handled}`);
  await channel.close();
  await conn.close();
})();
