package com.hermes.jobs.search;

import java.util.HashSet;
import java.util.Set;

public class JobIndexer {

    private static final Set<String> STACKS = Set.of(
            "java","spring","node","react","angular","vue","python","django","flask",
            "php","laravel","golang","kotlin","swift","docker","kubernetes","aws","sql"
    );

    private static final Set<String> LEVELS = Set.of(
            "estagio","intern","junior","jr","pleno","mid","senior","sr","lead","staff","principal"
    );

    private static final Set<String> WORK = Set.of(
            "remote","remoto","hybrid","hibrido","onsite","presencial"
    );

    public static IndexedData extract(String title, String description) {

        String text = (title + " " + description).toLowerCase();

        Set<String> stacks = new HashSet<>();
        Set<String> levels = new HashSet<>();
        Set<String> workMode = new HashSet<>();

        for (String s : STACKS)
            if (text.contains(s)) stacks.add(s);

        for (String l : LEVELS)
            if (text.contains(l)) levels.add(l);

        for (String w : WORK)
            if (text.contains(w)) workMode.add(w);

        return new IndexedData(stacks, levels, workMode);
    }
}