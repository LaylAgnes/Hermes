package com.hermes.jobs.search;

import java.util.HashSet;
import java.util.Set;

public class SearchCriteria {

    public Set<Seniority> seniorities = new HashSet<>();
    public Set<Area> areas = new HashSet<>();
    public Set<String> stacks = new HashSet<>();

    public boolean remote;
    public String country;

    public String rawText;
}