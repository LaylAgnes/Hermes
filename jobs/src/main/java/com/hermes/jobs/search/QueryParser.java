package com.hermes.jobs.search;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class QueryParser {

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private static final Set<String> STOP_WORDS = Set.of(
            "de", "da", "do", "das", "dos", "a", "o", "e", "para", "com", "em", "na", "no"
    );

    private static final Map<String, Seniority> SENIORITY_MAP = Map.ofEntries(
            Map.entry("estagio", Seniority.INTERN),
            Map.entry("estagiario", Seniority.INTERN),
            Map.entry("intern", Seniority.INTERN),
            Map.entry("trainee", Seniority.TRAINEE),
            Map.entry("junior", Seniority.JUNIOR),
            Map.entry("jr", Seniority.JUNIOR),
            Map.entry("pleno", Seniority.MID),
            Map.entry("mid", Seniority.MID),
            Map.entry("middle", Seniority.MID),
            Map.entry("senior", Seniority.SENIOR),
            Map.entry("sr", Seniority.SENIOR),
            Map.entry("staff", Seniority.STAFF),
            Map.entry("lead", Seniority.LEAD),
            Map.entry("principal", Seniority.PRINCIPAL),
            Map.entry("manager", Seniority.MANAGER)
    );

    private static final Map<String, Area> AREA_MAP = Map.ofEntries(
            Map.entry("backend", Area.BACKEND),
            Map.entry("frontend", Area.FRONTEND),
            Map.entry("fullstack", Area.FULLSTACK),
            Map.entry("full-stack", Area.FULLSTACK),
            Map.entry("mobile", Area.MOBILE),
            Map.entry("dados", Area.DATA),
            Map.entry("data", Area.DATA),
            Map.entry("devops", Area.DEVOPS),
            Map.entry("qa", Area.QA),
            Map.entry("security", Area.SECURITY),
            Map.entry("seguranca", Area.SECURITY)
    );

    private static final Set<String> STACKS = Set.of(
            "java", "spring", "node", "nodejs", "react", "angular", "vue", "python", "django",
            "flask", "php", "laravel", "golang", "go", "c#", ".net", "dotnet", "kotlin", "swift",
            "typescript", "javascript", "postgres", "postgresql", "mysql", "mongodb", "redis",
            "docker", "kubernetes", "aws", "gcp", "azure", "terraform", "ruby", "rails"
    );

    private static final Set<String> LOCATION_TERMS = Set.of(
            "brasil", "brazil", "portugal", "europe", "latam", "usa", "canada", "argentina", "mexico"
    );

    public static SearchCriteria parse(String query) {

        String normalized = normalize(query);

        SearchCriteria c = new SearchCriteria();
        c.rawText = normalized;

        if (normalized.contains("remote") || normalized.contains("remoto")) {
            c.remote = true;
            c.workModes.add("remote");
        }

        if (normalized.contains("hibrido") || normalized.contains("hybrid"))
            c.workModes.add("hybrid");

        if (normalized.contains("presencial") || normalized.contains("onsite") || normalized.contains("on-site"))
            c.workModes.add("onsite");

        for (var entry : SENIORITY_MAP.entrySet())
            if (containsToken(normalized, entry.getKey()))
                c.seniorities.add(entry.getValue());

        for (var entry : AREA_MAP.entrySet())
            if (containsToken(normalized, entry.getKey()))
                c.areas.add(entry.getValue());

        for (String stack : STACKS)
            if (containsToken(normalized, stack))
                c.stacks.add(stack.equals("dotnet") ? ".net" : stack);

        for (String location : LOCATION_TERMS)
            if (containsToken(normalized, location))
                c.locationTerms.add(location);

        if (c.locationTerms.contains("brasil") || c.locationTerms.contains("brazil"))
            c.country = "brazil";

        for (String token : normalized.split(" ")) {
            if (token.isBlank() || STOP_WORDS.contains(token))
                continue;

            if (!STACKS.contains(token)
                    && !SENIORITY_MAP.containsKey(token)
                    && !AREA_MAP.containsKey(token)
                    && !LOCATION_TERMS.contains(token)
                    && !token.equals("remote")
                    && !token.equals("remoto")
                    && !token.equals("hibrido")
                    && !token.equals("hybrid")
                    && !token.equals("onsite")
                    && !token.equals("presencial")) {
                c.freeTextTerms.add(token);
            }
        }

        return c;
    }

    private static String normalize(String text) {
        if (text == null)
            return "";

        String normalized = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^a-z0-9#.+\\-\\s]", " ");

        return MULTI_SPACE.matcher(normalized).replaceAll(" ").trim();
    }

    private static boolean containsToken(String normalizedQuery, String value) {
        String target = normalize(value);
        return Arrays.asList(normalizedQuery.split(" ")).contains(target);
    }
}
