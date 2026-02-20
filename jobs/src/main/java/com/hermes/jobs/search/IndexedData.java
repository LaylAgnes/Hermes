package com.hermes.jobs.search;

import java.util.Set;

public record IndexedData(
        Set<String> stacks,
        Set<String> levels,
        Set<String> workModes
) {}