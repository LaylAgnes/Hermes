package com.hermes.jobs.search;

import com.hermes.jobs.job.JobEntity;
import com.hermes.jobs.job.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int CANDIDATE_LIMIT = 500;
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private final JobRepository repository;
    private final QuerySynonymCatalog synonymCatalog;

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
                .map(job -> new ScoredJob(job, score(job, criteria)))
                .filter(scored -> scored.score() > 0)
                .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                .map(ScoredJob::job)
                .toList();

        int start = Math.toIntExact(pageable.getOffset());
        int end = Math.min(start + pageable.getPageSize(), ranked.size());

        if (start >= ranked.size()) {
            return new PageImpl<>(List.of(), pageable, ranked.size());
        }

        return new PageImpl<>(ranked.subList(start, end), pageable, ranked.size());
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

    private record ScoredJob(JobEntity job, int score) {
    }
}
