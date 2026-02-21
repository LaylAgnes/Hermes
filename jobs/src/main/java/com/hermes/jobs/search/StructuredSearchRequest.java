package com.hermes.jobs.search;

import java.util.Set;

public record StructuredSearchRequest(
        Set<String> stacks,
        Set<String> seniorities,
        Set<String> areas,
        Set<String> workModes,
        String language,
        String framework,
        String keyword,
        String location
) {
}
