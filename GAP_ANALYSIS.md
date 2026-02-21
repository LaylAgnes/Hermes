# Gap analysis da checklist de prontidão

Este documento consolida o status atual do repositório em relação à lista enviada (P0/P1/P2).

## P0 (crítico)

1. **DLQ com painel operacional**: **já atendido**.
   - Existe painel web para triagem e replay seletivo (`dlq-dashboard.js`) e script de replay via CLI.

2. **Idempotência forte (persistente/distribuída)**: **já atendido (com Redis)**.
   - Producer e consumer usam `createIdempotencyStore()` com `SET NX EX` no Redis.
   - Há modo degradado sem Redis apenas quando `IDEMPOTENCY_REQUIRED=false`.

3. **E2E completo com crawler real no fluxo**: **já atendido**.
   - Existe teste E2E com RabbitMQ real cobrindo fluxo producer -> queue -> consumer -> import API e cenário de DLQ.

## P1 (alto)

1. **Observabilidade de produção**: **faltando parcialmente**.
   - Há `/healthz` e `/metrics` JSON no producer e actuator básico no backend.
   - **Falta**: endpoint Prometheus, traces OpenTelemetry e regras de alerta operacional.

2. **Contrato de API versionado formalmente**: **faltando parcialmente**.
   - Há alias de rota (`/api/search` e `/api/v1/search`) e teste de endpoint versionado.
   - **Falta**: especificação OpenAPI por versão, política de depreciação/sunset, changelog semântico e testes de compatibilidade entre versões.

3. **Ampliação real de fontes**: **faltando**.
   - Catálogo atual define 4 fontes.
   - **Falta**: expansão por ATS e monitoramento de disponibilidade/taxa de sucesso por fonte.

## P2 (médio)

1. **Ranking evoluído**: **faltando**.
   - Ranking atual é heurístico por pesos e ocorrência de tokens.
   - **Falta**: benchmark offline (NDCG/MRR), feature logging e reranking por modelo.

2. **Frontend de produto (avançado)**: **faltando parcialmente**.
   - Já há chips, compartilhamento, ordenação, modal de detalhes e persistência básica.
   - **Falta**: estado de URL robusto/versionado, filtros salvos mais robustos, acessibilidade aprofundada e analytics comportamental.
