# SLO/SLA Operacional (Inicial)

## SLO propostos
- **Disponibilidade busca v1** (`/api/v1/search/filters`): 99.5% mensal.
- **Latência p95 busca v1**: <= 1.2s em janela de 5 minutos.
- **Taxa de erro 5xx busca v1**: < 1% por janela de 10 minutos.
- **Taxa de rejeição de import por source**: < 2% por janela de 15 minutos.
- **Acceptance rate por source (SLI de qualidade)**: >= 90% por janela de 15 minutos.

## SLIs
- `up{job="hermes-jobs"}`
- `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri="/api/v1/search/filters"}[5m])) by (le))`
- `rate(http_server_requests_seconds_count{uri="/api/v1/search/filters",status=~"5.."}[10m]) / rate(http_server_requests_seconds_count{uri="/api/v1/search/filters"}[10m])`
- `sum by (source, source_type) (increase(hermes_jobs_import_rejected_by_source_total[15m]))`
- `hermes_quality_source_acceptance_rate_15m`

> Fórmula de qualidade por source: `importados / recebidos` =
> `sum(increase(hermes_jobs_import_by_source_total[15m])) / sum(increase(hermes_consumer_received_by_source_total[15m]))`

## Política de resposta
- Violação crítica (disponibilidade): acionar incidente P1 em até 15 minutos.
- Violação de latência/erro: abrir investigação e rollback/mitigação em até 30 minutos.
- Violação de acceptance rate por source: abrir incidente de qualidade e acionar owner da integração ATS em até 30 minutos.
