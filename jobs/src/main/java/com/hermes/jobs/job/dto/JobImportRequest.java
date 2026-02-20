package com.hermes.jobs.job.dto;

import java.util.List;

public record JobImportRequest(
        List<JobDocument> jobs
) {}