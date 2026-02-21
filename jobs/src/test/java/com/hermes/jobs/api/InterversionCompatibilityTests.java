package com.hermes.jobs.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InterversionCompatibilityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldKeepAliasAndV1OptionsCompatible() throws Exception {
        String alias = mockMvc.perform(get("/api/search/options"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String v1 = mockMvc.perform(get("/api/v1/search/options"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(objectMapper.readTree(v1), objectMapper.readTree(alias));
    }

    @Test
    void shouldKeepAliasAndV1FiltersCompatible() throws Exception {
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

        String alias = mockMvc.perform(post("/api/search/filters?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchPayload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String v1 = mockMvc.perform(post("/api/v1/search/filters?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchPayload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(objectMapper.readTree(v1), objectMapper.readTree(alias));
    }
}
