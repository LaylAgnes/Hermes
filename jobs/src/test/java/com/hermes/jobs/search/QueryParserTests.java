package com.hermes.jobs.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryParserTests {

    @Test
    void shouldParseStacksAndWorkMode() {
        QuerySynonymCatalog catalog = new QuerySynonymCatalog();
        SearchCriteria criteria = QueryParser.parse("backend java remoto senior", catalog);

        assertTrue(criteria.stacks.contains("java"));
        assertTrue(criteria.workModes.contains("remote"));
        assertTrue(criteria.seniorities.contains(Seniority.SENIOR));
    }

    @Test
    void shouldUseConfigurableSynonyms() {
        QuerySynonymCatalog catalog = new QuerySynonymCatalog();
        catalog.setArea(java.util.Map.of("sre", "DEVOPS"));

        SearchCriteria criteria = QueryParser.parse("vaga sre", catalog);

        assertTrue(criteria.areas.contains(Area.DEVOPS));
    }
}
