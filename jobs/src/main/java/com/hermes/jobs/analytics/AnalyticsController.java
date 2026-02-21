package com.hermes.jobs.analytics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final MeterRegistry meterRegistry;

    public AnalyticsController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostMapping("/events")
    public ResponseEntity<Void> event(@RequestBody(required = false) Map<String, Object> payload) {
        String eventName = payload != null && payload.get("eventName") != null
                ? String.valueOf(payload.get("eventName"))
                : "unknown";

        meterRegistry.counter("hermes_frontend_events_total", "event", eventName).increment();
        return ResponseEntity.accepted().build();
    }
}
