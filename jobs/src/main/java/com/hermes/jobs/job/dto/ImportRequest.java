package com.hermes.jobs.job.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ImportRequest(
        @NotEmpty
        List<String> urls
) {}
