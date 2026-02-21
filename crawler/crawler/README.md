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
- `RUN_MODE` (`batch`/`continuous`, default: `batch`)
- `POLL_INTERVAL_MS` (default: `900000`)
- `MAX_JOBS_PER_SOURCE` (default: `30`)
- `NAV_TIMEOUT_MS` (default: `45000`)
- `REQUEST_TIMEOUT_MS` (default: `30000`)
- `API_RETRIES` (default: `3`)
- `MAX_SOURCE_RETRIES` (default: `2`)
- `BATCH_SIZE` (default: `50`)
- `DLQ_PATH` (default: `./dlq.jsonl`)
- `QUEUE_PATH` (default: `./queue.jsonl`)
- `METRICS_PORT` (ex.: `9090` para habilitar `/healthz` e `/metrics`)
- `PARSER_VERSION` (default: `v3`)

## Fontes/ATS suportadas

- `greenhouse` (API pública boards + detalhe de vaga)
- `lever` (API pública postings)
- `gupy` (scraping com Playwright)
- `workday` (scraping com Playwright)

## Operação

- Pipeline com fila persistida em disco (`QUEUE_PATH`) + envio em lotes para API.
- Retry por fonte e por envio API.
- DLQ JSONL para falhas de extração, validação e envio, com replay automático de falhas de API.
- Qualidade de dados por vaga (`sourceType`, `confidence`, `parserVersion`, `ingestionTraceId`).
- Endpoints operacionais opcionais (`/healthz`, `/metrics`) com métricas por fonte.
