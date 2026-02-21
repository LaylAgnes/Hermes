# Plano de execução para fechamento de gaps de produção

## 1) Diagnóstico consolidado (estado atual)

### P0 (bloqueantes)
1. **CI/CD automatizado inexistente**.
   - Não há pipeline versionado para build/test/lint/deploy.
2. **Higiene de repositório comprometida**.
   - Artefatos de build (`jobs/target`), metadados de IDE (`jobs/.idea`) e `node_modules` estão versionados.
3. **Perfil de produção do backend não formalizado**.
   - Configuração padrão ainda com H2 em memória e `ddl-auto: create`.
4. **Segurança e antiabuso de API ausentes**.
   - Endpoints públicos sem autenticação/autorização e sem camada explícita de rate limit.

### P1 (alto impacto)
1. **Empacotamento/runtime reprodutível incompleto**.
   - Não há Dockerfile(s) da aplicação e padrão de runtime imutável para backend/crawler.
2. **Release/rollback sem playbook formal de aplicação**.
   - Existe runbook de observabilidade, mas não playbook versionado de deploy/rollback.
3. **Catálogo de fontes ainda curto para escala comercial**.
4. **Qualidade operacional por fonte ainda parcial**.
   - Há observabilidade e alertas, mas falta automação de validação/score por integração para operação contínua.

### P2 (evolução)
1. Ranking ainda baseado em heurística com pesos manuais.
2. UX/produto e governança de engenharia/documentação raiz ainda incompletas para operação em escala.

---

## 2) Organização das tasks (roadmap recomendado)

## Fase 0 — Blindagem imediata (Semana 1)
**Objetivo:** zerar risco de regressão operacional básica.

- [ ] Criar `.gitignore` raiz para Java/Node/IDE e remover artefatos versionados do Git.
- [ ] Definir branch protection + checks obrigatórios (build, testes, lint).
- [ ] Criar pipeline CI mínimo:
  - backend: `mvn test`
  - crawler: `npm ci && npm test` (ou smoke equivalente)
- [ ] Congelar baseline de dependências e publicar política de atualização.

**Definition of Done (Fase 0)**
- PR não passa sem CI verde.
- Repositório sem `target/`, `.idea/`, `node_modules/` rastreados.

## Fase 1 — Produção segura (Semanas 2–3)
**Objetivo:** tornar backend publicável com risco controlado.

- [ ] Introduzir perfis `dev` e `prod` (arquivos separados).
- [ ] Produção com PostgreSQL obrigatório, migração de schema e `ddl-auto=validate`.
- [ ] Adicionar autenticação de API (JWT ou API key com rotação).
- [ ] Adicionar rate limiting e política antiabuso (por IP e/ou token).
- [ ] Endurecimento de headers e CORS por ambiente.

**Definition of Done (Fase 1)**
- Ambiente prod sobe sem H2.
- Chamadas sem credencial retornam 401/403.
- Burst abusivo retorna 429 de forma previsível.

## Fase 2 — Entregabilidade e operação (Semanas 4–5)
**Objetivo:** publicar e reverter com previsibilidade.

- [ ] Dockerfile backend + crawler (multi-stage).
- [ ] Compose/K8s de referência para runtime local/prod-like.
- [ ] Pipeline CD com promoção por ambiente (staging -> prod).
- [ ] Playbook de release/rollback versionado (incluindo critérios de go/no-go).
- [ ] Teste de rollback em ambiente de staging.

**Definition of Done (Fase 2)**
- Build reproduzível por tag.
- Release e rollback executáveis por runbook em < 30 min.

## Fase 3 — Escala comercial (Semanas 6–8)
**Objetivo:** elevar cobertura de mercado e qualidade contínua.

- [ ] Expandir catálogo para novas integrações ATS.
- [ ] Instrumentar score de qualidade por fonte (aceitação, latência, falha por motivo).
- [ ] Automação de gates por fonte (pausa/quarentena automática quando degradar).
- [ ] Evoluir benchmark de ranking para rotina contínua com dataset real.
- [ ] Consolidar governança: arquitetura, ownership, ADRs, SLOs e calendário de revisão.

**Definition of Done (Fase 3)**
- Queda de qualidade por fonte detectada e mitigada automaticamente.
- Ranking com avaliação recorrente (MRR/NDCG) e histórico comparável.

---

## 3) Priorização prática (ordem de execução)

1. **P0-1 Higiene + CI** (sem isso, qualquer avanço vira risco de regressão).
2. **P0-2 Perfil prod + segurança API** (sem isso, não é publicável).
3. **P1-1 Runtime reprodutível + playbook release/rollback**.
4. **P1-2 Expansão de fontes + automação de qualidade por integração**.
5. **P2 Evoluções de ranking/UX/governança**.

---

## 4) Estimativa de prontidão real

### Referência atual
- **Demo interna:** ~85% (mantida).
- **Produção com risco controlado:** **~60%** (mantida e coerente com os gaps P0/P1).

### Quanto falta para “realmente pronto” (produção)
- Para chegar em **~80% de prontidão de produção**: concluir Fases 0, 1 e parte principal da Fase 2.
- Para chegar em **~90%+ (operação robusta)**: concluir Fases 0–3 com automação de qualidade por fonte e governança ativa.

### Esforço estimado
- **Até ~80%:** 4–6 semanas (time enxuto, execução focada).
- **Até ~90%+:** 8–10 semanas (incluindo escala de fontes + maturidade operacional).

---

## 5) Checklist executivo (saída)

- [ ] CI obrigatório em PR + proteção de branch.
- [ ] Repositório higienizado e política de versionamento de artefatos.
- [ ] Perfil `prod` seguro (sem H2, sem `ddl-auto:create`).
- [ ] Auth + rate limit + antiabuso em produção.
- [ ] Imagens/versionamento reprodutíveis (backend/crawler).
- [ ] Playbook de release e rollback testado.
- [ ] Qualidade por fonte com automação operacional.
- [ ] Ranking/UX/governança com trilha contínua de evolução.
