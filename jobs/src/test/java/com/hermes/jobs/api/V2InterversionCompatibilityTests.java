package com.hermes.jobs.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
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
class V2InterversionCompatibilityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldKeepV1AndV2OptionsCompatibleWhenV2Exists() throws Exception {
        int v2Status = mockMvc.perform(get("/api/v2/search/options"))
                .andReturn().getResponse().getStatus();

        Assumptions.assumeTrue(v2Status != 404, "v2 ainda não implementada");

        String v1 = mockMvc.perform(get("/api/v1/search/options"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String v2 = mockMvc.perform(get("/api/v2/search/options"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(objectMapper.readTree(v1), objectMapper.readTree(v2));
    }

    @Test
    void shouldKeepV1AndV2StructuredSearchCompatibleWhenV2Exists() throws Exception {
        int v2Status = mockMvc.perform(post("/api/v2/search/filters?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();

        Assumptions.assumeTrue(v2Status != 404, "v2 ainda não implementada");

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

        String v1 = mockMvc.perform(post("/api/v1/search/filters?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchPayload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String v2 = mockMvc.perform(post("/api/v2/search/filters?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchPayload)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(objectMapper.readTree(v1), objectMapper.readTree(v2));
    }
}
