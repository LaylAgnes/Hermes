package com.hermes.jobs.search;

import java.util.*;

public class QueryParser {

    private static final Map<String, Seniority> SENIORITY_MAP = Map.ofEntries(
            Map.entry("estagio", Seniority.INTERN),
            Map.entry("intern", Seniority.INTERN),
            Map.entry("trainee", Seniority.TRAINEE),
            Map.entry("junior", Seniority.JUNIOR),
            Map.entry("jr", Seniority.JUNIOR),
            Map.entry("pleno", Seniority.MID),
            Map.entry("mid", Seniority.MID),
            Map.entry("senior", Seniority.SENIOR),
            Map.entry("sr", Seniority.SENIOR),
            Map.entry("lead", Seniority.LEAD),
            Map.entry("tech lead", Seniority.LEAD)
    );

    private static final Map<String, Area> AREA_MAP = Map.ofEntries(
            Map.entry("backend", Area.BACKEND),
            Map.entry("frontend", Area.FRONTEND),
            Map.entry("fullstack", Area.FULLSTACK),
            Map.entry("mobile", Area.MOBILE),
            Map.entry("dados", Area.DATA),
            Map.entry("data", Area.DATA),
            Map.entry("devops", Area.DEVOPS),
            Map.entry("qa", Area.QA),
            Map.entry("security", Area.SECURITY)
    );

    private static final Set<String> STACKS = Set.of(
            "java","spring","node","react","angular","vue","python","django",
            "flask","php","laravel","golang","c#",".net","kotlin","swift",
            "typescript","javascript","postgres","mysql","mongodb","redis",
            "docker","kubernetes","aws","gcp","azure"
    );

    public static SearchCriteria parse(String query) {

        String normalized = query.toLowerCase();

        SearchCriteria c = new SearchCriteria();
        c.rawText = normalized;

        if (normalized.contains("remote") || normalized.contains("remoto"))
            c.remote = true;

        if (normalized.contains("brasil") || normalized.contains("brazil"))
            c.country = "brazil";

        for (var entry : SENIORITY_MAP.entrySet())
            if (normalized.contains(entry.getKey()))
                c.seniorities.add(entry.getValue());

        for (var entry : AREA_MAP.entrySet())
            if (normalized.contains(entry.getKey()))
                c.areas.add(entry.getValue());

        for (String stack : STACKS)
            if (normalized.contains(stack))
                c.stacks.add(stack);

        return c;
    }
}