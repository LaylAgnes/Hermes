# Changelog

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
