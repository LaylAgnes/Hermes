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

        if (request.stacks() != null) {
            request.stacks().stream()
                    .map(value -> value.toLowerCase(Locale.ROOT).trim())
                    .filter(value -> !value.isBlank())
                    .forEach(criteria.stacks::add);
        }

        if (request.seniorities() != null)
            criteria.seniorities.addAll(request.seniorities());

        if (request.areas() != null)
            criteria.areas.addAll(request.areas());

        if (request.workModes() != null) {
            request.workModes().stream()
                    .map(value -> value.toLowerCase(Locale.ROOT).trim())
                    .filter(value -> !value.isBlank())
                    .forEach(criteria.workModes::add);
        }

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

        if (request.stacks() != null)
            request.stacks().forEach(tokens::add);

        if (request.workModes() != null)
            request.workModes().forEach(tokens::add);

        if (request.seniorities() != null)
            request.seniorities().forEach(value -> tokens.add(value.name()));

        if (request.areas() != null)
            request.areas().forEach(value -> tokens.add(value.name()));

        return String.join(" ", tokens);
    }
}
