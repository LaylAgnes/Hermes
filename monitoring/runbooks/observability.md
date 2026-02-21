# Runbook de Observabilidade (Hermes)

## Objetivo
Orientar resposta rápida para incidentes de ingestão/search usando métricas de crawler + backend.

## Dashboards sugeridos
- **Crawler ingestão**: taxa de publicação, retries, DLQ e disponibilidade por source.
- **Jobs API**: RPS, 5xx, latência p95 e disponibilidade.

## Alertas e ação imediata

### 1) `ConsumerDown`
1. Verificar processo do consumer (`npm run consumer`) e conectividade RabbitMQ.
2. Conferir `/healthz` no `CONSUMER_METRICS_PORT`.
3. Se reiniciado, observar `hermes_consumer_processing_errors_total` e fila DLQ.

### 2) `ProducerSourceFailuresHigh`
1. Identificar fonte com falha usando `hermes_producer_source_failures_by_source_total`.
2. Validar `hermes_producer_source_up{source=...}`.
3. Desabilitar temporariamente source problemática (se necessário) e abrir correção do parser.

### 3) `JobsSearch5xxHigh`
1. Confirmar aumento de 5xx em `/api/v1/search/filters`.
2. Correlacionar com traces (`traceparent`) de requisições recentes.
3. Verificar disponibilidade de banco e logs do serviço jobs.

### 4) `JobsHighLatencyP95`
1. Comparar p95 com taxa de requests.
2. Validar uso de CPU/memória e volume de dataset.
3. Ajustar paginação/filtros e avaliar tuning do ranking.

## Coleta mínima recomendada
- Scrape Prometheus: `hermes-producer`, `hermes-consumer`, `hermes-jobs`.
- Retenção mínima: 14 dias para análise de tendência.
