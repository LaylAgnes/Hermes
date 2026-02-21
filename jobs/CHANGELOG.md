# Changelog

## [1.4.0] - 2026-02-21
### Added
- Infra de observabilidade em `monitoring/stack` (Prometheus + Grafana + Tempo + OTel Collector).
- Métricas fim-a-fim por source no consumer e no backend (`hermes_consumer_*_by_source_total`, `hermes_jobs_import_by_source_total`).
- Testes de compatibilidade interversão entre alias e v1 (`InterversionCompatibilityTests`).
- Melhorias de frontend: estado de URL versionado, analytics events e atributos de acessibilidade.

## [1.3.0] - 2026-02-21
### Added
- Hardening operacional de observabilidade com alertas backend (`monitoring/prometheus-alerts-backend.yml`).
- Dashboard Grafana inicial para operação conjunta crawler + jobs (`monitoring/grafana/hermes-ops-dashboard.json`).
- Runbook operacional de incidentes (`monitoring/runbooks/observability.md`).

## [1.2.0] - 2026-02-21
### Added
- Contrato OpenAPI v1 detalhado com schemas de request/response e endpoints de busca/import.
- Contract tests para validar presença/estrutura do arquivo OpenAPI e compatibilidade das respostas reais da API v1.

## [1.1.0] - 2026-02-21
### Added
- Exposição de métricas Prometheus e tracing OTLP no serviço `jobs`.
- Política formal de versionamento/depreciação em `docs/API_VERSIONING.md`.
- Contrato OpenAPI da v1 em `docs/api/openapi-v1.yaml`.
- Headers de depreciação para alias não versionado `/api/search`.

## [1.0.0] - 2026-02-20
### Added
- Base da API de busca e ingestão de vagas.
