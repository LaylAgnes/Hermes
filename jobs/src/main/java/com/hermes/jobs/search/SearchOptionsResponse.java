package com.hermes.jobs.search;

import java.util.List;

public record SearchOptionsResponse(
        List<Seniority> seniorities,
        List<Area> areas,
        List<String> workModes,
        List<String> languages,
        List<String> frameworks
) {
}
