package com.hermes.jobs.analytics;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/events")
    public ResponseEntity<Void> event(@RequestBody(required = false) Map<String, Object> payload) {
        String eventName = payload != null && payload.get("eventName") != null
                ? String.valueOf(payload.get("eventName"))
                : "unknown";

        analyticsService.registerEvent(eventName, payload == null ? Map.of() : payload);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/funnel")
    public ResponseEntity<Map<String, Object>> funnel(
            @RequestParam(name = "since", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since
    ) {
        Instant effectiveSince = since == null ? Instant.now().minus(7, ChronoUnit.DAYS) : since;
        return ResponseEntity.ok(analyticsService.funnel(effectiveSince));
    }

    @GetMapping("/cohort")
    public ResponseEntity<Map<String, Object>> cohort(@RequestParam(name = "days", defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.cohort(Math.max(days, 1)));
    }
}
