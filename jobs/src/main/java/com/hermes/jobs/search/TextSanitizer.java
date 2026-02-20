package com.hermes.jobs.search;

public class TextSanitizer {

    public static String clean(String raw) {

        if (raw == null) return "";

        String text = raw;

        // remove múltiplos espaços
        text = text.replaceAll("\\s+", " ");

        // remove seções de candidatura
        text = text.replaceAll("(?i)apply(.*?)submit application", "");
        text = text.replaceAll("(?i)candidatar a esta vaga(.*?)enviar inscrição", "");

        // remove labels de formulário
        text = text.replaceAll("(?i)(nome|sobrenome|email|telefone|currículo|resume|cover letter).*", "");

        // remove menus
        text = text.replaceAll("(?i)back to jobs", "");
        text = text.replaceAll("(?i)voltar para vagas", "");

        return text.trim();
    }
}