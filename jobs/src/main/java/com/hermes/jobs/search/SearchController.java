package com.hermes.jobs.search;

import com.hermes.jobs.job.dto.JobResponse;
import com.hermes.jobs.job.mapper.JobMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService service;

    @PostMapping
    public Page<JobResponse> search(@RequestBody @Valid SearchRequest request, Pageable pageable) {
        return service.search(request.query(), pageable)
                .map(JobMapper::toResponse);
    }

    @GetMapping
    public Page<JobResponse> searchByQueryParam(@RequestParam("q") String query, Pageable pageable) {
        return service.search(query, pageable)
                .map(JobMapper::toResponse);
    }

    @PostMapping("/filters")
    public Page<JobResponse> searchWithFilters(@RequestBody StructuredSearchRequest request, Pageable pageable) {
        SearchCriteria criteria = new SearchCriteria();

        normalizeSet(request.stacks()).forEach(criteria.stacks::add);
        normalizeSet(request.workModes()).forEach(criteria.workModes::add);

        normalizeSet(request.seniorities()).stream()
                .map(this::parseSeniority)
                .flatMap(Optional::stream)
                .forEach(criteria.seniorities::add);

        normalizeSet(request.areas()).stream()
                .map(this::parseArea)
                .flatMap(Optional::stream)
                .forEach(criteria.areas::add);

        appendFreeTerm(criteria, request.language());
        appendFreeTerm(criteria, request.framework());
        appendFreeTerm(criteria, request.keyword());
        appendFreeTerm(criteria, request.location());

        if (request.location() != null && !request.location().isBlank())
            criteria.locationTerms.add(request.location().toLowerCase(Locale.ROOT).trim());

        criteria.rawText = buildRawText(request);

        return service.search(criteria, pageable).map(JobMapper::toResponse);
    }

    @GetMapping("/options")
    public SearchOptionsResponse options() {
        return new SearchOptionsResponse(
                List.of(Seniority.values()),
                List.of(Area.values()),
                List.of("remote", "hybrid", "onsite"),
                List.of("java", "python", "javascript", "typescript", "go", "c#", "kotlin", "php"),
                List.of("spring", "react", "angular", "vue", "django", "flask", "laravel", "dotnet", "nodejs")
        );
    }

    private Set<String> normalizeSet(Set<String> values) {
        if (values == null)
            return Set.of();

        return values.stream()
                .map(value -> value == null ? "" : value.toLowerCase(Locale.ROOT).trim())
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Optional<Seniority> parseSeniority(String value) {
        String normalized = value.toUpperCase(Locale.ROOT);

        if ("JR".equals(normalized))
            normalized = "JUNIOR";
        else if ("SR".equals(normalized))
            normalized = "SENIOR";
        else if ("PLENO".equals(normalized) || "MIDDLE".equals(normalized))
            normalized = "MID";

        try {
            return Optional.of(Seniority.valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Area> parseArea(String value) {
        String normalized = value.toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        try {
            return Optional.of(Area.valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private void appendFreeTerm(SearchCriteria criteria, String value) {
        if (value == null)
            return;

        String normalized = value.toLowerCase(Locale.ROOT).trim();
        if (!normalized.isBlank())
            criteria.freeTextTerms.add(normalized);
    }

    private String buildRawText(StructuredSearchRequest request) {
        Set<String> tokens = new LinkedHashSet<>();

        if (request.keyword() != null && !request.keyword().isBlank())
            tokens.add(request.keyword().trim());

        if (request.language() != null && !request.language().isBlank())
            tokens.add(request.language().trim());

        if (request.framework() != null && !request.framework().isBlank())
            tokens.add(request.framework().trim());

        if (request.location() != null && !request.location().isBlank())
            tokens.add(request.location().trim());

        normalizeSet(request.stacks()).forEach(tokens::add);
        normalizeSet(request.workModes()).forEach(tokens::add);
        normalizeSet(request.seniorities()).forEach(tokens::add);
        normalizeSet(request.areas()).forEach(tokens::add);

        return String.join(" ", tokens);
    }
}
