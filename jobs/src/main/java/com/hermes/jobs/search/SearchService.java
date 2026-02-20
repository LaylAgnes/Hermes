package com.hermes.jobs.search;

import com.hermes.jobs.job.JobEntity;
import com.hermes.jobs.job.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int CANDIDATE_LIMIT = 500;
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private final JobRepository repository;

    public Page<JobEntity> search(String query, Pageable pageable) {

        SearchCriteria criteria = QueryParser.parse(query);

        if (criteria.rawText == null || criteria.rawText.isBlank()) {
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
            if (containsToken(stacks, stack) || containsToken(title, stack) || containsToken(description, stack))
                score += 12;
        }

        for (var area : c.areas) {
            String areaToken = area.name().toLowerCase(Locale.ROOT);
            if (containsToken(title, areaToken) || containsToken(description, areaToken))
                score += 8;
        }

        for (var requestedSeniority : c.seniorities) {
            String seniorityToken = requestedSeniority.name().toLowerCase(Locale.ROOT);
            if (containsToken(seniority, seniorityToken) || containsToken(title, seniorityToken) || containsToken(description, seniorityToken))
                score += 10;
        }

        for (String workModeTerm : c.workModes) {
            if (containsToken(workMode, workModeTerm) || containsToken(title, workModeTerm) || containsToken(description, workModeTerm))
                score += 7;
        }

        for (String term : c.locationTerms) {
            if (containsToken(location, term) || containsToken(description, term) || containsToken(title, term))
                score += 6;
        }

        for (String token : c.freeTextTerms) {
            if (containsToken(title, token)) {
                score += 9;
            } else if (containsToken(description, token) || containsToken(company, token)) {
                score += 4;
            }
        }

        if (c.stacks.isEmpty() && c.areas.isEmpty() && c.seniorities.isEmpty() && c.workModes.isEmpty() && c.locationTerms.isEmpty() && c.freeTextTerms.isEmpty()) {
            score = 1;
        }

        return score;
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
