package com.hermes.jobs.job;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ImportMetricsService {

    private final MeterRegistry meterRegistry;

    public ImportMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void markImported(String sourceName, String sourceType) {
        meterRegistry.counter(
                "hermes_jobs_import_by_source_total",
                "source", sourceName == null ? "unknown" : sourceName,
                "source_type", sourceType == null ? "unknown" : sourceType
        ).increment();
    }

    public void markImportRejected(String sourceName, String sourceType, String reason) {
        meterRegistry.counter(
                "hermes_jobs_import_rejected_by_source_total",
                "source", sourceName == null ? "unknown" : sourceName,
                "source_type", sourceType == null ? "unknown" : sourceType,
                "reason", reason == null ? "unknown" : reason
        ).increment();
    }
}
