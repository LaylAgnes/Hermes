package com.hermes.jobs.search;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

public class JobClassifier {

    // ================= STACKS =================

    private static final Set<String> STACKS = Set.of(
            "java","spring","kotlin",
            "python","django","flask","fastapi",
            "node","nodejs","typescript","javascript",
            "react","angular","vue","next","nestjs",
            "c#",".net","dotnet",
            "golang","go",
            "php","laravel",
            "ruby","rails",
            "docker","kubernetes","terraform",
            "aws","azure","gcp",
            "sql","postgres","postgresql","mysql","mongodb","redis",
            "html","css","sass","tailwind",
            "android","ios","swift","flutter","react-native"
    );

    // ================= SENIORIDADE =================

    private static final Map<String, Pattern> SENIORITY = Map.of(
            "intern", Pattern.compile("\\b(estagio|estagiario|intern(ship)?)\\b"),
            "junior", Pattern.compile("\\b(junior|jr)\\b"),
            "mid", Pattern.compile("\\b(pleno|mid|middle)\\b"),
            "senior", Pattern.compile("\\b(senior|sr)\\b"),
            "lead", Pattern.compile("\\b(lead|tech lead|principal|staff|head)\\b")
    );

    private static final List<String> SENIORITY_ORDER =
            List.of("intern","junior","mid","senior","lead");


    // ================= WORK MODE =================

    private static final Map<String, Pattern> WORKMODE = Map.of(
            "remote", Pattern.compile("\\b(100%\\s*)?(remote|remoto|work\\s*from\\s*home|anywhere)\\b"),
            "hybrid", Pattern.compile("\\b(hybrid|hibrido|híbrido)\\b"),
            "onsite", Pattern.compile("\\b(onsite|on-site|presencial)\\b")
    );


    // ================= CLASSIFY =================

    public static ClassificationResult classify(String rawText) {

        String text = normalize(rawText);

        Set<String> foundStacks = detectStacks(text);
        String seniority = detectSeniority(text);
        String workMode = detectWorkMode(text);

        return new ClassificationResult(foundStacks, seniority, workMode);
    }


    // ================= DETECT STACKS =================

    private static Set<String> detectStacks(String text) {

        Set<String> found = new HashSet<>();

        for (String stack : STACKS) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(stack) + "\\b");
            if (p.matcher(text).find())
                found.add(stack);
        }

        return found;
    }


    // ================= DETECT SENIORITY =================

    private static String detectSeniority(String text) {

        String levelFound = null;

        for (String level : SENIORITY_ORDER) {
            Pattern p = SENIORITY.get(level);
            if (p.matcher(text).find())
                levelFound = level; // sempre guarda o maior
        }

        return levelFound;
    }


    // ================= DETECT WORKMODE =================

    private static String detectWorkMode(String text) {

        for (var e : WORKMODE.entrySet())
            if (e.getValue().matcher(text).find())
                return e.getKey();

        return null;
    }


    // ================= NORMALIZE =================

    private static String normalize(String text) {

        if (text == null) return "";

        text = text.toLowerCase();

        text = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        return text;
    }


    // ================= RESULT =================

    public record ClassificationResult(
            Set<String> stacks,
            String seniority,
            String workMode
    ) {}
}