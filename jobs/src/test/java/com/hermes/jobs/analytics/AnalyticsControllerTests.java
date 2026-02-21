package com.hermes.jobs.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class AnalyticsControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAcceptAnalyticsEvent() throws Exception {
        mockMvc.perform(post("/api/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("eventName", "search", "sessionId", "s1"))))
                .andExpect(status().isAccepted());
    }

    @Test
    void shouldReturnFunnelData() throws Exception {
        mockMvc.perform(post("/api/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("eventName", "page_view", "sessionId", "s1"))))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/analytics/funnel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps").isArray());
    }

    @Test
    void shouldReturnCohortData() throws Exception {
        mockMvc.perform(get("/api/analytics/cohort?days=7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").exists());
    }
}
