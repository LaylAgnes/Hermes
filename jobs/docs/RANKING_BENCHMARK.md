# Ranking Benchmark (P2)

## Métricas adotadas
- **MRR@K**: prioriza quão cedo aparece o primeiro item relevante.
- **NDCG@K**: mede qualidade da ordenação considerando posição dos relevantes.

## Implementação atual
- Rerank linear por features no `SearchService`:
  - heuristic score
  - title hits
  - description hits
  - stack hits
  - seniority match
  - freshness (dias)
- Pesos configuráveis em `search.ranking.*` no `application.yml`.

## Testes offline
- `RankingBenchmarkTests` valida:
  1. sanidade de cálculo de MRR/NDCG;
  2. cenário em que rerank melhora baseline.

## Próximos passos
1. Montar dataset real rotulado (query -> relevantes).
2. Rodar benchmark por corte temporal (semanal).
3. Ajustar pesos e comparar com baseline.
4. Evoluir de linear para modelo treinado (LTR/reranker).
