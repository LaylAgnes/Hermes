package com.hermes.jobs.search;

import java.util.Set;

public record StructuredSearchRequest(
        Set<String> stacks,
        Set<Seniority> seniorities,
        Set<Area> areas,
        Set<String> workModes,
        String language,
        String framework,
        String keyword,
        String location
) {
}
