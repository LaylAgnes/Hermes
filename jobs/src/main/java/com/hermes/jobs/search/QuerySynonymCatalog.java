package com.hermes.jobs.search;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

@Getter
@Component
@ConfigurationProperties(prefix = "search.synonyms")
public class QuerySynonymCatalog {

    private Map<String, String> seniority = new LinkedHashMap<>();
    private Map<String, String> area = new LinkedHashMap<>();
    private Set<String> stacks = new LinkedHashSet<>();
    private Set<String> locations = new LinkedHashSet<>();

    public void setSeniority(Map<String, String> seniority) {
        this.seniority = seniority;
    }

    public void setArea(Map<String, String> area) {
        this.area = area;
    }

    public void setStacks(Set<String> stacks) {
        this.stacks = stacks;
    }

    public void setLocations(Set<String> locations) {
        this.locations = locations;
    }

    public Map<String, Seniority> seniorityMap() {
        Map<String, Seniority> map = new LinkedHashMap<>();
        defaultSeniority().forEach((k, v) -> map.put(k, Seniority.valueOf(v)));
        seniority.forEach((k, v) -> map.put(normalizeKey(k), Seniority.valueOf(v.toUpperCase(Locale.ROOT))));
        return map;
    }

    public Map<String, Area> areaMap() {
        Map<String, Area> map = new LinkedHashMap<>();
        defaultArea().forEach((k, v) -> map.put(k, Area.valueOf(v)));
        area.forEach((k, v) -> map.put(normalizeKey(k), Area.valueOf(v.toUpperCase(Locale.ROOT))));
        return map;
    }

    public Set<String> stacksSet() {
        Set<String> result = new LinkedHashSet<>(defaultStacks());
        stacks.stream().map(this::normalizeKey).forEach(result::add);
        return result;
    }

    public Set<String> locationTermsSet() {
        Set<String> result = new LinkedHashSet<>(defaultLocations());
        locations.stream().map(this::normalizeKey).forEach(result::add);
        return result;
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private Map<String, String> defaultSeniority() {
        return Map.ofEntries(
                Map.entry("estagio", "INTERN"),
                Map.entry("estagiario", "INTERN"),
                Map.entry("intern", "INTERN"),
                Map.entry("trainee", "TRAINEE"),
                Map.entry("junior", "JUNIOR"),
                Map.entry("jr", "JUNIOR"),
                Map.entry("pleno", "MID"),
                Map.entry("mid", "MID"),
                Map.entry("middle", "MID"),
                Map.entry("senior", "SENIOR"),
                Map.entry("sr", "SENIOR"),
                Map.entry("staff", "STAFF"),
                Map.entry("lead", "LEAD"),
                Map.entry("principal", "PRINCIPAL"),
                Map.entry("manager", "MANAGER")
        );
    }

    private Map<String, String> defaultArea() {
        return Map.ofEntries(
                Map.entry("backend", "BACKEND"),
                Map.entry("frontend", "FRONTEND"),
                Map.entry("fullstack", "FULLSTACK"),
                Map.entry("full-stack", "FULLSTACK"),
                Map.entry("mobile", "MOBILE"),
                Map.entry("dados", "DATA"),
                Map.entry("data", "DATA"),
                Map.entry("devops", "DEVOPS"),
                Map.entry("qa", "QA"),
                Map.entry("security", "SECURITY"),
                Map.entry("seguranca", "SECURITY")
        );
    }

    private Set<String> defaultStacks() {
        return Set.of(
                "java", "spring", "node", "nodejs", "react", "angular", "vue", "python", "django",
                "flask", "php", "laravel", "golang", "go", "c#", ".net", "dotnet", "kotlin", "swift",
                "typescript", "javascript", "postgres", "postgresql", "mysql", "mongodb", "redis",
                "docker", "kubernetes", "aws", "gcp", "azure", "terraform", "ruby", "rails"
        );
    }

    private Set<String> defaultLocations() {
        return Set.of("brasil", "brazil", "portugal", "europe", "latam", "usa", "canada", "argentina", "mexico");
    }
}
