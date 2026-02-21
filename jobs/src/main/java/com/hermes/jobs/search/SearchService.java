package com.hermes.jobs.search;

import com.hermes.jobs.job.JobEntity;
import com.hermes.jobs.job.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private static final int CANDIDATE_LIMIT = 500;
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private final JobRepository repository;
    private final QuerySynonymCatalog synonymCatalog;
    private final RankingProperties rankingProperties;

    public Page<JobEntity> search(String query, Pageable pageable) {
        SearchCriteria criteria = QueryParser.parse(query, synonymCatalog);
        return search(criteria, pageable);
    }

    public Page<JobEntity> search(SearchCriteria criteria, Pageable pageable) {

        if (criteria == null || criteria.rawText == null || criteria.rawText.isBlank()) {
            return repository.findByActiveTrue(pageable);
        }

        Pageable candidatePage = PageRequest.of(0, CANDIDATE_LIMIT, Sort.by(Sort.Direction.DESC, "coletadoEm"));

        List<JobEntity> ranked = repository.findAll(JobSearchSpecifications.from(criteria), candidatePage)
                .stream()
                .map(job -> {
                    int heuristicScore = score(job, criteria);
                    RankingFeatures f = buildFeatures(job, criteria, heuristicScore);
                    double rerankScore = rerank(f);
                    maybeLogFeatures(job, f, rerankScore);
                    return new ScoredJob(job, rerankScore);
                })
                .filter(scored -> scored.score() > 0)
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .map(ScoredJob::job)
                .toList();

        int start = Math.toIntExact(pageable.getOffset());
        int end = Math.min(start + pageable.getPageSize(), ranked.size());

        if (start >= ranked.size()) {
            return new PageImpl<>(List.of(), pageable, ranked.size());
        }

        return new PageImpl<>(ranked.subList(start, end), pageable, ranked.size());
    }

    private void maybeLogFeatures(JobEntity job, RankingFeatures f, double rerankScore) {
        if (!rankingProperties.isFeatureLoggingEnabled())
            return;

        log.info("[ranking] traceId={} title='{}' heuristic={} titleHits={} descHits={} stackHits={} seniorityMatch={} freshnessDays={} rerank={}",
                job.getIngestionTraceId(), job.getTitle(), f.heuristicScore(), f.titleHits(), f.descriptionHits(),
                f.stackHits(), f.seniorityMatch(), f.freshnessDays(), rerankScore);
    }

    private RankingFeatures buildFeatures(JobEntity job, SearchCriteria c, int heuristicScore) {
        String title = normalize(job.getTitle());
        String description = normalize(job.getDescription());
        String stacks = normalize(job.getStacks());
        String seniority = normalize(job.getSeniority());

        int titleHits = 0;
        int descriptionHits = 0;
        int stackHits = 0;

        Set<String> allTokens = new HashSet<>();
        allTokens.addAll(c.stacks);
        allTokens.addAll(c.freeTextTerms);

        for (String token : allTokens) {
            titleHits += countOccurrences(title, token);
            descriptionHits += countOccurrences(description, token);
            stackHits += countOccurrences(stacks, token);
        }

        int seniorityMatch = 0;
        for (var requested : c.seniorities) {
            if (seniority.contains(requested.name().toLowerCase(Locale.ROOT))) {
                seniorityMatch = 1;
                break;
            }
        }

        long freshnessDays = 365;
        if (job.getColetadoEm() != null) {
            freshnessDays = Math.max(0, ChronoUnit.DAYS.between(job.getColetadoEm(), OffsetDateTime.now()));
        }

        return new RankingFeatures(
                heuristicScore,
                titleHits,
                descriptionHits,
                stackHits,
                seniorityMatch,
                freshnessDays
        );
    }

    private double rerank(RankingFeatures f) {
        return (f.heuristicScore() * rankingProperties.getWeightHeuristic())
                + (f.titleHits() * rankingProperties.getWeightTitleHits())
                + (f.descriptionHits() * rankingProperties.getWeightDescriptionHits())
                + (f.stackHits() * rankingProperties.getWeightStackHits())
                + (f.seniorityMatch() * rankingProperties.getWeightSeniorityMatch())
                - (f.freshnessDays() * rankingProperties.getWeightFreshnessDays());
    }

    private int score(JobEntity job, SearchCriteria c) {

        int score = 0;

        String title = normalize(job.getTitle());
        String description = normalize(job.getDescription());
        String stacks = normalize(job.getStacks());
        String seniority = normalize(job.getSeniority());
        String workMode = normalize(job.getWorkMode());
        String location = normalize(job.getLocation());
        String company = normalize(job.getEmpresa());

        for (String stack : c.stacks) {
            score += tokenScore(stack, 16, title, stacks, description);
        }

        for (var area : c.areas) {
            String areaToken = area.name().toLowerCase(Locale.ROOT);
            score += tokenScore(areaToken, 10, title, description);
        }

        for (var requestedSeniority : c.seniorities) {
            String seniorityToken = requestedSeniority.name().toLowerCase(Locale.ROOT);
            score += tokenScore(seniorityToken, 12, seniority, title, description);
        }

        for (String workModeTerm : c.workModes) {
            score += tokenScore(workModeTerm, 8, workMode, title, description);
        }

        for (String term : c.locationTerms) {
            score += tokenScore(term, 8, location, description, title);
        }

        for (String token : c.freeTextTerms) {
            int titleHits = countOccurrences(title, token);
            int descriptionHits = countOccurrences(description, token);
            int companyHits = countOccurrences(company, token);

            score += (titleHits * 8) + (descriptionHits * 3) + (companyHits * 4);
        }

        if (!c.freeTextTerms.isEmpty()) {
            int matchedTerms = (int) c.freeTextTerms.stream().filter(token -> containsToken(title + " " + description, token)).count();
            double coverageBoost = (double) matchedTerms / c.freeTextTerms.size();
            score += (int) Math.round(coverageBoost * 20);
        }

        if (c.stacks.isEmpty() && c.areas.isEmpty() && c.seniorities.isEmpty() && c.workModes.isEmpty() && c.locationTerms.isEmpty() && c.freeTextTerms.isEmpty()) {
            score = 1;
        }

        return score;
    }

    private int tokenScore(String token, int baseWeight, String... fields) {
        int result = 0;
        for (int i = 0; i < fields.length; i++) {
            int hits = countOccurrences(fields[i], token);
            if (hits == 0) continue;

            int fieldWeight = switch (i) {
                case 0 -> baseWeight;
                case 1 -> Math.max(1, baseWeight - 4);
                default -> Math.max(1, baseWeight - 8);
            };

            result += hits * fieldWeight;
        }

        return result;
    }

    private int countOccurrences(String text, String token) {
        if (text == null || text.isBlank() || token == null || token.isBlank()) return 0;

        int count = 0;
        int index = 0;
        String normalizedToken = token.toLowerCase(Locale.ROOT);

        while ((index = text.indexOf(normalizedToken, index)) >= 0) {
            count++;
            index += normalizedToken.length();
        }

        return count;
    }

    private String normalize(String input) {
        if (input == null)
            return "";

        String normalized = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9#.+\\-\\s]", " ");

        return MULTI_SPACE.matcher(normalized).replaceAll(" ").trim();
    }

    private boolean containsToken(String text, String token) {
        return text.contains(token.toLowerCase(Locale.ROOT));
    }

    record RankingFeatures(
            int heuristicScore,
            int titleHits,
            int descriptionHits,
            int stackHits,
            int seniorityMatch,
            long freshnessDays
    ) {
    }

    private record ScoredJob(JobEntity job, double score) {
    }
}
