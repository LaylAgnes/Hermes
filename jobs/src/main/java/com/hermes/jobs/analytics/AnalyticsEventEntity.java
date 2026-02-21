package com.hermes.jobs.analytics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_events")
public class AnalyticsEventEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String eventName;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 3000)
    private String payload;

    protected AnalyticsEventEntity() {
    }

    public AnalyticsEventEntity(String eventName, String sessionId, String payload) {
        this.id = UUID.randomUUID();
        this.eventName = eventName;
        this.sessionId = sessionId;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public String getEventName() { return eventName; }
    public String getSessionId() { return sessionId; }
    public Instant getCreatedAt() { return createdAt; }
}
