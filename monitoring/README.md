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

## Configs
- `stack/prometheus.yml`
- `stack/otel-collector-config.yaml`
- `stack/tempo.yaml`
- alertas backend: `prometheus-alerts-backend.yml`
- alertas crawler: `../crawler/crawler/monitoring/prometheus-alerts.yml`
