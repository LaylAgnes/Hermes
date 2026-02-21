const http = require('http');
const { URL } = require('url');
const {
  QUEUE_DLQ,
  connectRabbit,
  parseMessage,
  publishJob
} = require('./lib/rabbitBus');

const RABBIT_URL = process.env.RABBIT_URL || 'amqp://localhost';
const PORT = Number(process.env.DLQ_DASHBOARD_PORT || 8091);
const MAX_SCAN = Number(process.env.DLQ_DASHBOARD_MAX_SCAN || 500);

function json(res, status, data) {
  res.writeHead(status, { 'content-type': 'application/json' });
  res.end(JSON.stringify(data));
}

function matches(entry, source, errorContains, fromIso, toIso) {
  if (source && entry?.payload?.sourceName !== source) return false;
  if (errorContains && !String(entry?.reason || '').toLowerCase().includes(errorContains.toLowerCase())) return false;
  if (fromIso || toIso) {
    const t = new Date(entry?.at || '').getTime();
    if (Number.isNaN(t)) return false;
    if (fromIso && t < new Date(fromIso).getTime()) return false;
    if (toIso && t > new Date(toIso).getTime()) return false;
  }
  return true;
}

async function scanDlq(channel, { limit, source, errorContains, fromIso, toIso }) {
  const q = await channel.checkQueue(QUEUE_DLQ);
  const scanCount = Math.min(q.messageCount, MAX_SCAN);
  const buffered = [];
  const filtered = [];

  for (let i = 0; i < scanCount; i++) {
    const msg = await channel.get(QUEUE_DLQ, { noAck: false });
    if (!msg) break;
    const entry = parseMessage(msg);
    buffered.push(msg);
    if (matches(entry, source, errorContains, fromIso, toIso) && filtered.length < limit) {
      filtered.push({
        at: entry.at,
        reason: entry.reason,
        retryCount: entry.retryCount,
        payload: entry.payload
      });
    }
    channel.ack(msg);
  }

  for (const msg of buffered) {
    channel.sendToQueue(QUEUE_DLQ, msg.content, msg.properties);
  }

  return { scanned: buffered.length, items: filtered, total: q.messageCount };
}

async function replay(channel, { limit, source, errorContains, fromIso, toIso }) {
  const q = await channel.checkQueue(QUEUE_DLQ);
  const scanCount = Math.min(q.messageCount, MAX_SCAN);
  const buffered = [];
  let replayed = 0;

  for (let i = 0; i < scanCount; i++) {
    const msg = await channel.get(QUEUE_DLQ, { noAck: false });
    if (!msg) break;
    const entry = parseMessage(msg);

    if (replayed < limit && matches(entry, source, errorContains, fromIso, toIso) && entry?.payload) {
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

  return { replayed, scanned: scanCount };
}

function page() {
  return `<!doctype html>
<html><head><meta charset="utf-8"/><title>Hermes DLQ</title>
<style>body{font-family:Arial,sans-serif;margin:24px}table{width:100%;border-collapse:collapse}th,td{border:1px solid #ddd;padding:8px}input{margin-right:8px}</style></head>
<body><h1>DLQ Operacional</h1>
<div>
  <input id="source" placeholder="sourceName"/>
  <input id="error" placeholder="erro contém"/>
  <input id="from" placeholder="from ISO" size="24"/>
  <input id="to" placeholder="to ISO" size="24"/>
  <input id="limit" placeholder="limit" value="50" size="5"/>
  <button onclick="load()">Buscar</button>
  <button onclick="replay()">Replay Selecionados</button>
</div>
<p id="meta"></p>
<table><thead><tr><th>Quando</th><th>Fonte</th><th>Erro</th><th>URL</th></tr></thead><tbody id="rows"></tbody></table>
<script>
function filters(){return {source:source.value,errorContains:error.value,fromIso:from.value,toIso:to.value,limit:Number(limit.value||50)}}
async function load(){const f=filters();const q=new URLSearchParams(f);const r=await fetch('/api/dlq?'+q);const d=await r.json();meta.textContent=` + "`total=${d.total} scanned=${d.scanned} shown=${d.items.length}`" + `;rows.innerHTML=d.items.map(i=>'<tr><td>'+ (i.at||'') +'</td><td>'+ (i.payload?.sourceName||'') +'</td><td>'+ (i.reason||'') +'</td><td><a href="'+(i.payload?.url||'#')+'" target="_blank">'+(i.payload?.url||'')+'</a></td></tr>').join('')}
async function replay(){const r=await fetch('/api/replay',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify(filters())});const d=await r.json();alert('replayed='+d.replayed+' scanned='+d.scanned);load();}
load();
</script></body></html>`;
}

(async () => {
  const { channel } = await connectRabbit(RABBIT_URL);

  const server = http.createServer(async (req, res) => {
    const u = new URL(req.url, `http://${req.headers.host}`);

    if (req.method === 'GET' && u.pathname === '/') {
      res.writeHead(200, { 'content-type': 'text/html; charset=utf-8' });
      res.end(page());
      return;
    }

    if (req.method === 'GET' && u.pathname === '/api/dlq') {
      const limit = Number(u.searchParams.get('limit') || 50);
      const payload = {
        limit,
        source: u.searchParams.get('source') || '',
        errorContains: u.searchParams.get('errorContains') || '',
        fromIso: u.searchParams.get('fromIso') || '',
        toIso: u.searchParams.get('toIso') || ''
      };
      const data = await scanDlq(channel, payload);
      json(res, 200, data);
      return;
    }

    if (req.method === 'POST' && u.pathname === '/api/replay') {
      let body = '';
      req.on('data', chunk => body += chunk.toString('utf8'));
      req.on('end', async () => {
        const payload = JSON.parse(body || '{}');
        const data = await replay(channel, {
          limit: Number(payload.limit || 50),
          source: payload.source || '',
          errorContains: payload.errorContains || '',
          fromIso: payload.fromIso || '',
          toIso: payload.toIso || ''
        });
        json(res, 200, data);
      });
      return;
    }

    json(res, 404, { error: 'not found' });
  });

  server.listen(PORT, '0.0.0.0', () => {
    console.log(`dlq dashboard listening on :${PORT}`);
  });
})();
