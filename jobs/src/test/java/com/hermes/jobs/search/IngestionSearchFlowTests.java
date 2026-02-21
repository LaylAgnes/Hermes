package com.hermes.jobs.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hermes.jobs.job.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IngestionSearchFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    void shouldImportAndSearchThroughVersionedFiltersEndpoint() throws Exception {
        Map<String, Object> importPayload = Map.of(
                "jobs", java.util.List.of(
                        Map.of(
                                "url", "https://jobs.lever.co/acme/123",
                                "title", "Senior Backend Engineer",
                                "location", "Brazil",
                                "description", "Strong Java and Spring experience for backend platform.",
                                "sourceType", "lever",
                                "sourceName", "acme-lever",
                                "confidence", 0.93,
                                "parserVersion", "v3",
                                "ingestionTraceId", "trace-e2e-1"
                        )
                )
        );

        mockMvc.perform(post("/api/jobs/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(importPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));

        Map<String, Object> searchPayload = Map.of(
                "stacks", java.util.List.of("java"),
                "seniorities", java.util.List.of("senior"),
                "areas", java.util.List.of("backend"),
                "workModes", java.util.List.of(),
                "language", "java",
                "framework", "spring",
                "keyword", "backend",
                "location", "brazil"
        );

        mockMvc.perform(post("/api/v1/search/filters?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Senior Backend Engineer"))
                .andExpect(jsonPath("$.content[0].sourceType").value("lever"))
                .andExpect(jsonPath("$.content[0].parserVersion").value("v3"));
    }
}
