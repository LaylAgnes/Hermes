# Runbook de Observabilidade

## Alertmanager
- URL local: `http://localhost:9093`
- Rotas:
  - `severity=critical` => PagerDuty + Slack
  - `severity=warning` => Slack
  - fallback => e-mail

## Fluxo de triagem
1. Abrir alerta no Alertmanager e coletar labels (`alertname`, `job`, `source`, `source_type`).
2. Validar painel `Hermes Ops - Ingestion + Search` no Grafana.
3. Para alertas por source, priorizar:
   - `hermes_quality_source_acceptance_rate_15m`
   - `hermes_consumer_received_by_source_total`
   - `hermes_jobs_import_by_source_total`
   - `hermes_jobs_import_rejected_by_source_total`
4. Executar mitigação por tipo:
   - indisponibilidade da API: rollback/restore imediato.
   - latência/erro: reduzir carga, investigar regressões e dependências.
   - quality drop por source: pausar source problemática e acionar owner ATS.

## Playbook de Quality SLI por ATS
- **Sinal**: `HermesSourceAcceptanceRateLow`.
- **Objetivo**: retornar acceptance rate >= 90% em até 30 minutos.
- **Ações rápidas**:
  1. Verificar se houve aumento de schema drift na source.
  2. Inspecionar `reason` em `hermes_jobs_import_rejected_by_source_total`.
  3. Aplicar patch de parser/normalização ou fallback de campos obrigatórios.
  4. Reprocessar lote recente para recuperar backlog.

## Pós-incidente
- Registrar causa raiz e tempo de recuperação.
- Atualizar limiares/alertas se necessário.
- Gerar ticket de prevenção (parser robusto, contrato com ATS, testes adicionais).
