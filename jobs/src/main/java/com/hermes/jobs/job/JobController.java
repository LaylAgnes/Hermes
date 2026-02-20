package com.hermes.jobs.job;

import com.hermes.jobs.job.dto.ImportRequest;
import com.hermes.jobs.job.dto.JobImportRequest;
import com.hermes.jobs.job.dto.JobResponse;
import com.hermes.jobs.job.mapper.JobMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService service;

    @PostMapping("/import")
    public int importJobs(@RequestBody JobImportRequest request) {
        return service.importDocuments(request);
    }

    @GetMapping
    public Page<JobResponse> list(Pageable pageable) {
        return service.list(pageable).map(JobMapper::toResponse);
    }

    @GetMapping("/domain/{domain}")
    public Page<JobResponse> byDomain(@PathVariable String domain, Pageable pageable) {
        return service.byDomain(domain, pageable).map(JobMapper::toResponse);
    }

    @GetMapping("/search")
    public Page<JobResponse> search(@RequestParam String q, Pageable pageable) {
        return service.search(q, pageable).map(JobMapper::toResponse);
    }

    @GetMapping("/source/{source}")
    public Page<JobResponse> bySource(@PathVariable String source, Pageable pageable) {
        return service.bySource(source, pageable).map(JobMapper::toResponse);
    }
}