package com.hermes.jobs.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final AnalyticsEventRepository repository;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    public AnalyticsService(AnalyticsEventRepository repository, MeterRegistry meterRegistry, ObjectMapper objectMapper) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    public void registerEvent(String eventName, Map<String, Object> payload) {
        String sessionId = payload != null && payload.get("sessionId") != null
                ? String.valueOf(payload.get("sessionId"))
                : UUID.randomUUID().toString();

        repository.save(new AnalyticsEventEntity(eventName, sessionId, toJson(payload)));
        meterRegistry.counter("hermes_frontend_events_total", "event", eventName).increment();
    }

    public Map<String, Object> funnel(Instant since) {
        long visits = repository.countByEventNameAndCreatedAtAfter("page_view", since);
        long searches = repository.countByEventNameAndCreatedAtAfter("search", since);
        long details = repository.countByEventNameAndCreatedAtAfter("open_details", since);
        long applies = repository.countByEventNameAndCreatedAtAfter("apply_click", since);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("since", since.toString());
        response.put("steps", List.of(
                Map.of("name", "page_view", "count", visits),
                Map.of("name", "search", "count", searches),
                Map.of("name", "open_details", "count", details),
                Map.of("name", "apply_click", "count", applies)
        ));
        return response;
    }

    public Map<String, Object> cohort(int days) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        List<Object[]> rows = repository.aggregateByEventName(since);
        Map<String, Object> byEvent = new LinkedHashMap<>();
        for (Object[] row : rows) {
            byEvent.put(String.valueOf(row[0]), Map.of("events", row[1], "sessions", row[2]));
        }
        return Map.of("since", since.toString(), "events", byEvent);
    }

    private String toJson(Map<String, Object> payload) {
        if (payload == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
