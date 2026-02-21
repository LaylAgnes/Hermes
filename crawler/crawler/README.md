# Hermes Crawler

Crawler multi-fonte para coletar vagas e enviar para a API `jobs`.

## Executar

```bash
npm install
npm run start
```

## Variáveis de ambiente

- `API_URL` (default: `http://localhost:8080/api/jobs/import`)
- `HEADLESS` (`true`/`false`, default: `true`)
- `MAX_JOBS_PER_SOURCE` (default: `30`)
- `NAV_TIMEOUT_MS` (default: `45000`)
- `REQUEST_TIMEOUT_MS` (default: `30000`)
- `API_RETRIES` (default: `3`)

## Fontes

As fontes ficam em `sources.js`.
Hoje o crawler já aceita várias fontes Greenhouse e deduplica por URL antes do envio.
