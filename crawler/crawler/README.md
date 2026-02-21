# Hermes Crawler

Crawler multi-fonte com **fila real RabbitMQ** para coletar vagas e enviar para a API `jobs`.

## Executar

```bash
npm install
npm run producer        # coleta e publica mensagens
npm run consumer        # consome mensagens e envia para API
npm run dlq-dashboard   # painel operacional para triagem/replay da DLQ
```

Replay de DLQ via CLI com filtros:

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
- `METRICS_PORT` (ex.: `9090` para `/healthz`, `/metrics` [Prometheus] e `/metrics/json` no producer)
- `CONSUMER_METRICS_PORT` (ex.: `9091` para `/healthz`, `/metrics` [Prometheus] e `/metrics/json` no consumer)
- `PARSER_VERSION` (default: `v4`)
- `OTEL_EXPORTER_OTLP_ENDPOINT` (opcional, para coletor OTLP quando tracing estiver habilitado no backend)

Idempotência distribuída (Redis):
- `IDEMPOTENCY_REDIS_URL` (default: `redis://localhost:6379`)
- `IDEMPOTENCY_KEY_PREFIX` (default: `hermes:idempotency`)
- `IDEMPOTENCY_TTL_SECONDS` (default: `604800`)
- `IDEMPOTENCY_REQUIRED` (default: `true`; use `false` para modo degradado sem Redis)

Replay seletivo da DLQ (CLI e painel):
- `REPLAY_SOURCE`
- `REPLAY_ERROR_CONTAINS`
- `REPLAY_FROM` / `REPLAY_TO` (ISO-8601)
- `REPLAY_LIMIT`
- `REPLAY_SCAN_LIMIT`

Painel DLQ:
- `DLQ_DASHBOARD_PORT` (default: `8091`)
- `DLQ_DASHBOARD_MAX_SCAN` (default: `500`)

## Fontes/ATS suportadas

- `greenhouse` (API boards + endpoint de detalhe)
- `lever` (API postings)
- `gupy` (scraping com Playwright)
- `workday` (scraping com Playwright)

Atualmente o catálogo vem com 8 fontes de exemplo distribuídas nesses ATS.

## Operação

- Arquitetura producer/consumer separada com RabbitMQ.
- Retry por mensagem no consumer e DLQ gerenciada no broker.
- Replay controlado da DLQ por critérios (source/erro/janela de tempo) via CLI e painel web.
- Idempotência forte por `url + ingestionTraceId` com chave distribuída em Redis.
- Propagação de contexto de tracing (`traceparent`) entre producer -> RabbitMQ -> consumer -> API.

## Teste E2E (crawler no fluxo)

```bash
npm run test:e2e
```

Valida o fluxo com broker RabbitMQ real: producer -> fila AMQP -> consumer -> `POST /api/jobs/import`, incluindo cenário de roteamento para DLQ em falha de import.

> Requer RabbitMQ acessível em `RABBIT_URL` (default `amqp://localhost`).


## Alertas (exemplo Prometheus)

Há um conjunto inicial de alertas em `monitoring/prometheus-alerts.yml` cobrindo:
- Consumer indisponível (`ConsumerDown`)
- Erro de processamento no consumer (`ConsumerProcessingErrors`)
- Envio para DLQ acima do limiar (`ConsumerDlqRateHigh`)
- Falha de coleta no producer (`ProducerSourceFailuresHigh`)

As métricas de producer agora incluem disponibilidade e sucesso por source (`hermes_producer_source_up`, `hermes_producer_source_success_total`, `hermes_producer_source_jobs_collected_total`).

O consumer também expõe métricas fim-a-fim por source (`hermes_consumer_imported_by_source_total`, `hermes_consumer_retried_by_source_total`, `hermes_consumer_dlq_by_source_total`).


## Operação integrada (crawler + backend)

- Alertas backend: `../monitoring/prometheus-alerts-backend.yml`
- Dashboard Grafana sugerido: `../monitoring/grafana/hermes-ops-dashboard.json`
- Runbook operacional: `../monitoring/runbooks/observability.md`


## Stack de observabilidade (produção/local)

Suba uma stack mínima (Prometheus + Grafana + Tempo + OTel Collector):

```bash
cd ../monitoring/stack
docker compose -f docker-compose.observability.yml up -d
```

Arquivos principais:
- `../monitoring/stack/prometheus.yml`
- `../monitoring/stack/otel-collector-config.yaml`
- `../monitoring/stack/tempo.yaml`
