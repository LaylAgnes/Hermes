package com.hermes.jobs.search;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class QueryParser {

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private static final Set<String> STOP_WORDS = Set.of(
            "de", "da", "do", "das", "dos", "a", "o", "e", "para", "com", "em", "na", "no"
    );

    public static SearchCriteria parse(String query, QuerySynonymCatalog catalog) {

        String normalized = normalize(query);
        Map<String, Seniority> seniorityMap = catalog.seniorityMap();
        Map<String, Area> areaMap = catalog.areaMap();
        Set<String> stacks = catalog.stacksSet();
        Set<String> locationTerms = catalog.locationTermsSet();

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

        for (var entry : seniorityMap.entrySet())
            if (containsToken(normalized, entry.getKey()))
                c.seniorities.add(entry.getValue());

        for (var entry : areaMap.entrySet())
            if (containsToken(normalized, entry.getKey()))
                c.areas.add(entry.getValue());

        for (String stack : stacks)
            if (containsToken(normalized, stack))
                c.stacks.add(stack.equals("dotnet") ? ".net" : stack);

        for (String location : locationTerms)
            if (containsToken(normalized, location))
                c.locationTerms.add(location);

        if (c.locationTerms.contains("brasil") || c.locationTerms.contains("brazil"))
            c.country = "brazil";

        for (String token : normalized.split(" ")) {
            if (token.isBlank() || STOP_WORDS.contains(token))
                continue;

            if (!stacks.contains(token)
                    && !seniorityMap.containsKey(token)
                    && !areaMap.containsKey(token)
                    && !locationTerms.contains(token)
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
