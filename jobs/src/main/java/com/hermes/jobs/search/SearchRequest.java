package com.hermes.jobs.search;

import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
        @NotBlank
        String query
) {}