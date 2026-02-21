let createClient = null;
try {
  ({ createClient } = require('redis'));
} catch {
  createClient = null;
}

const REDIS_URL = process.env.IDEMPOTENCY_REDIS_URL || 'redis://localhost:6379';
const KEY_PREFIX = process.env.IDEMPOTENCY_KEY_PREFIX || 'hermes:idempotency';
const TTL_SECONDS = Number(process.env.IDEMPOTENCY_TTL_SECONDS || 60 * 60 * 24 * 7);
const REQUIRED = process.env.IDEMPOTENCY_REQUIRED !== 'false';

function composeKey(scope, identity) {
  return `${KEY_PREFIX}:${scope}:${identity}`;
}

async function createIdempotencyStore() {
  if (!createClient) {
    if (REQUIRED) {
      throw new Error('redis package not installed; run npm install to enable distributed idempotency');
    }
    return {
      async claim() { return true; },
      async close() {}
    };
  }

  const client = createClient({ url: REDIS_URL });
  client.on('error', err => {
    console.error('[idempotency] redis error:', err.message);
  });

  try {
    await client.connect();
  } catch (error) {
    if (REQUIRED) {
      throw error;
    }
    console.warn('[idempotency] redis unavailable; idempotency disabled');
    return {
      async claim() { return true; },
      async close() {}
    };
  }

  return {
    async claim(scope, identity) {
      const result = await client.set(composeKey(scope, identity), '1', {
        NX: true,
        EX: TTL_SECONDS
      });
      return result === 'OK';
    },
    async close() {
      await client.quit();
    }
  };
}

module.exports = { createIdempotencyStore };
