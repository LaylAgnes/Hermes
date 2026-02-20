package com.hermes.jobs.job.dto;

public record JobDocument(
        String url,
        String title,
        String location,
        String description
) {}