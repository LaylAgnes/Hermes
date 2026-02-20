package com.hermes.jobs.search;

import java.util.*;

public class JobTextAnalyzer {

    private static final Set<String> STACKS = Set.of(
            "java","spring","kotlin","node","react","angular","vue",
            "python","django","flask","php","laravel","golang",
            "docker","kubernetes","aws","gcp","azure",
            "sql","postgres","mysql","mongodb","redis",
            "html","css","javascript","typescript"
    );

    private static final Map<String,String> SENIORITY = Map.ofEntries(
            Map.entry("estagio","internship"),
            Map.entry("intern","internship"),
            Map.entry("trainee","trainee"),
            Map.entry("junior","junior"),
            Map.entry("pleno","mid"),
            Map.entry("senior","senior"),
            Map.entry("staff","staff"),
            Map.entry("lead","lead"),
            Map.entry("principal","principal")
    );

    public static AnalyzedJob analyze(String text) {

        String normalized = text.toLowerCase();

        Set<String> stacksFound = new HashSet<>();
        for (String stack : STACKS)
            if (normalized.contains(stack))
                stacksFound.add(stack);

        String seniority = null;
        for (var entry : SENIORITY.entrySet())
            if (normalized.contains(entry.getKey()))
                seniority = entry.getValue();

        String workMode = "onsite";
        if (normalized.contains("remoto") || normalized.contains("remote"))
            workMode = "remote";
        else if (normalized.contains("hybrid") || normalized.contains("híbrido"))
            workMode = "hybrid";

        return new AnalyzedJob(
                String.join(",", stacksFound),
                seniority,
                workMode
        );
    }
}