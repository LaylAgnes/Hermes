package com.hermes.jobs.api;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractCompatibilityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobRepository repository;

    @BeforeEach
    void clear() {
        repository.deleteAll();
    }

    @Test
    void shouldRespectOptionsContractForV1() throws Exception {
        mockMvc.perform(get("/api/v1/search/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seniorities").isArray())
                .andExpect(jsonPath("$.areas").isArray())
                .andExpect(jsonPath("$.workModes").isArray())
                .andExpect(jsonPath("$.languages").isArray())
                .andExpect(jsonPath("$.frameworks").isArray());
    }

    @Test
    void shouldRespectStructuredSearchPageContractForV1() throws Exception {
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
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.size").isNumber())
                .andExpect(jsonPath("$.number").isNumber());
    }

    @Test
    void shouldRespectTextSearchRequestContractForV1() throws Exception {
        Map<String, Object> payload = Map.of("query", "java spring");

        mockMvc.perform(post("/api/v1/search?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }
}
