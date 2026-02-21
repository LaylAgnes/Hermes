package com.hermes.jobs.job.dto;

import jakarta.validation.constraints.NotBlank;

public record JobDocument(
        @NotBlank
        String url,
        String title,
        String location,
        String description,
        String sourceType,
        String sourceName,
        Double confidence,
        String parserVersion,
        String ingestionTraceId
) {}
