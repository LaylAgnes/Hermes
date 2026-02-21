# API Versioning & Deprecation Policy

## Versionamento
- Rotas estáveis devem ser publicadas em `/api/v{N}`.
- Rotas sem versão (`/api/search`) são apenas alias temporário de compatibilidade.

## Depreciação
- Ao anunciar depreciação, o alias não versionado passa a responder com headers:
  - `Deprecation: true`
  - `Sunset: <RFC-1123 date>`
  - `Link: <policy-url>; rel="deprecation"`
- O período mínimo recomendado até sunset é de 6 meses.

## Compatibilidade
- Mudanças breaking exigem nova versão major de rota (`/api/v2`).
- Mudanças backward-compatible entram na mesma versão com atualização de contrato OpenAPI e changelog.

## Contratos
- `docs/api/openapi-v1.yaml`: contrato oficial da versão v1.


## Testes de compatibilidade interversão
- `InterversionCompatibilityTests` valida equivalência entre alias (`/api/search`) e rotas versionadas (`/api/v1/search`) para opções e filtros.
