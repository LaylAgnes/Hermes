package com.hermes.jobs.job.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String url,
        String empresa,
        String domain,
        String source,
        String title,
        String location,
        String description,
        String stacks,
        String seniority,
        String workMode,
        OffsetDateTime coletadoEm,
        boolean active
) {}
