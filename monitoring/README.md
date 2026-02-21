# Observability Stack (Prometheus + Grafana + Tempo + OTel Collector)

## Subir stack local
```bash
cd monitoring/stack
docker compose -f docker-compose.observability.yml up -d
```

## Componentes
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`
- Tempo: `http://localhost:3200`
- OTel Collector: `4317` (gRPC) / `4318` (HTTP)
- Alertmanager: `http://localhost:9093`

## Configs
- `stack/prometheus.yml`
- `stack/otel-collector-config.yaml`
- `stack/tempo.yaml`
- alertas backend e SLI de qualidade: `prometheus-alerts-backend.yml`
- alertas crawler: `../crawler/crawler/monitoring/prometheus-alerts.yml`

## Alertmanager em canais reais
A configuração já suporta Slack/PagerDuty/e-mail via variáveis de ambiente (sem placeholder de webhook fixo):
- `ALERTMANAGER_SLACK_API_URL`
- `ALERTMANAGER_SLACK_CHANNEL`
- `ALERTMANAGER_PAGERDUTY_ROUTING_KEY`
- `ALERTMANAGER_EMAIL_TO`
- `ALERTMANAGER_EMAIL_FROM`
- `ALERTMANAGER_SMARTHOST`
- `ALERTMANAGER_SMTP_USER`
- `ALERTMANAGER_SMTP_PASSWORD`

## Promoção para produção (IaC)
- Helm chart: `deploy/helm/hermes-observability`
- Values por ambiente: `deploy/helm/environments/*-values.yaml`
- Terraform módulo e ambientes (dev/staging/prod): `deploy/terraform/**`

Exemplo (Helm):
```bash
helm upgrade --install hermes-observability-prod monitoring/deploy/helm/hermes-observability \
  -f monitoring/deploy/helm/environments/prod-values.yaml
```

## Grafana provisioning automático
- Datasources provisionadas: `stack/grafana-provisioning/datasources/datasources.yml`
- Dashboard provider: `stack/grafana-provisioning/dashboards/dashboards.yml`

## SLO/SLA
- Metas iniciais em `SLO.md`.
