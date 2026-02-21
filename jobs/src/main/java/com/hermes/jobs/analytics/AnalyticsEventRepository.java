package com.hermes.jobs.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEventEntity, UUID> {

    long countByEventNameAndCreatedAtAfter(String eventName, Instant createdAt);

    @Query("""
            select e.eventName, count(e), count(distinct e.sessionId)
            from AnalyticsEventEntity e
            where e.createdAt >= :since
            group by e.eventName
            """)
    List<Object[]> aggregateByEventName(Instant since);
}
