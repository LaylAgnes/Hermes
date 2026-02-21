# Hermes Crawler

Crawler multi-fonte com **fila real RabbitMQ** para coletar vagas e enviar para a API `jobs`.

## Executar

```bash
npm install
npm run producer   # coleta e publica mensagens
npm run consumer   # consome mensagens e envia para API
```

Replay de DLQ com filtros:

```bash
REPLAY_SOURCE=vtex-lever REPLAY_ERROR_CONTAINS=timeout npm run replay
```

## Variáveis de ambiente

- `RABBIT_URL` (default: `amqp://localhost`)
- `RABBIT_QUEUE_JOBS` (default: `hermes.jobs`)
- `RABBIT_QUEUE_DLQ` (default: `hermes.jobs.dlq`)
- `API_URL` (default: `http://localhost:8080/api/jobs/import`)
- `HEADLESS` (`true`/`false`, default: `true`)
- `RUN_MODE` (`batch`/`continuous`, default: `batch`)
- `POLL_INTERVAL_MS` (default: `900000`)
- `MAX_JOBS_PER_SOURCE` (default: `30`)
- `NAV_TIMEOUT_MS` (default: `45000`)
- `REQUEST_TIMEOUT_MS` (default: `30000`)
- `API_RETRIES` (default: `3`)
- `MAX_SOURCE_RETRIES` (default: `2`)
- `METRICS_PORT` (ex.: `9090` para `/healthz` e `/metrics` no producer)
- `PARSER_VERSION` (default: `v4`)

Replay seletivo da DLQ:
- `REPLAY_SOURCE`
- `REPLAY_ERROR_CONTAINS`
- `REPLAY_FROM` / `REPLAY_TO` (ISO-8601)
- `REPLAY_LIMIT`

## Fontes/ATS suportadas

- `greenhouse` (API boards + endpoint de detalhe)
- `lever` (API postings)
- `gupy` (scraping com Playwright)
- `workday` (scraping com Playwright)

## Operação

- Arquitetura producer/consumer separada com RabbitMQ.
- Retry por mensagem no consumer e DLQ gerenciada no broker.
- Replay controlado da DLQ por critérios (source/erro/janela de tempo).
- Idempotência por `url + ingestionTraceId`.
