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
- `RUN_MODE` (`batch`/`continuous`, default: `batch`)
- `POLL_INTERVAL_MS` (default: `900000`)
- `MAX_SOURCE_RETRIES` (default: `2`)
- `DLQ_PATH` (default: `./dlq.jsonl`)

## Fontes

As fontes ficam em `sources.js`.

Suporta `greenhouse`, `lever`, `workday` e `gupy`.

## Operação

- Validação por qualidade de dados (campos obrigatórios e confiança).
- Deduplicação por URL antes de enviar.
- Retry por fonte/API.
- DLQ em JSONL para falhas de extração, validação e envio.
- Métricas básicas por execução no log (`[metrics]`).
