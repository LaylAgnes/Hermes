package com.hermes.jobs.search;

import com.hermes.jobs.job.dto.JobResponse;
import com.hermes.jobs.job.mapper.JobMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

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
}
