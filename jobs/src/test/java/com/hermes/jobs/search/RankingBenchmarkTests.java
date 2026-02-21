package com.hermes.jobs.search;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RankingBenchmarkTests {

    @Test
    void shouldComputeExpectedNdcgAndMrr() {
        List<String> ranked = List.of("A", "B", "C", "D", "E");
        Set<String> relevant = Set.of("B", "D");

        double mrr = mrrAtK(ranked, relevant, 10);
        double ndcg = ndcgAtK(ranked, relevant, 5);

        assertTrue(mrr > 0.49 && mrr < 0.51, "MRR esperado ~0.5");
        assertTrue(ndcg > 0.64 && ndcg < 0.66, "NDCG esperado ~0.65");
    }

    @Test
    void shouldRewardBetterRanking() {
        List<String> baseline = List.of("A", "B", "C", "D");
        List<String> reranked = List.of("B", "D", "A", "C");
        Set<String> relevant = Set.of("B", "D");

        double baselineNdcg = ndcgAtK(baseline, relevant, 4);
        double rerankNdcg = ndcgAtK(reranked, relevant, 4);
        double baselineMrr = mrrAtK(baseline, relevant, 4);
        double rerankMrr = mrrAtK(reranked, relevant, 4);

        assertTrue(rerankNdcg > baselineNdcg);
        assertTrue(rerankMrr >= baselineMrr);
    }

    static double mrrAtK(List<String> rankedIds, Set<String> relevant, int k) {
        for (int i = 0; i < Math.min(k, rankedIds.size()); i++) {
            if (relevant.contains(rankedIds.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0.0;
    }

    static double ndcgAtK(List<String> rankedIds, Set<String> relevant, int k) {
        double dcg = 0.0;
        for (int i = 0; i < Math.min(k, rankedIds.size()); i++) {
            int rel = relevant.contains(rankedIds.get(i)) ? 1 : 0;
            dcg += (Math.pow(2, rel) - 1) / log2(i + 2);
        }

        int idealRelCount = Math.min(k, relevant.size());
        double idcg = 0.0;
        for (int i = 0; i < idealRelCount; i++) {
            idcg += (Math.pow(2, 1) - 1) / log2(i + 2);
        }

        if (idcg == 0.0) return 0.0;
        return dcg / idcg;
    }

    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }
}
