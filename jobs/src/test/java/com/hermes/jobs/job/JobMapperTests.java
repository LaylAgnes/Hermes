package com.hermes.jobs.job;

import com.hermes.jobs.job.mapper.JobMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobMapperTests {

    @Test
    void shouldMapGovernanceFields() {
        JobEntity entity = JobEntity.builder()
                .url("https://example.com/job")
                .empresa("example")
                .domain("example.com")
                .source("greenhouse")
                .sourceType("greenhouse")
                .sourceName("example-board")
                .confidence(0.88)
                .parserVersion("v2")
                .ingestionTraceId("trace-1")
                .coletadoEm(OffsetDateTime.now())
                .active(true)
                .build();

        var response = JobMapper.toResponse(entity);

        assertEquals("greenhouse", response.sourceType());
        assertEquals("example-board", response.sourceName());
        assertEquals(0.88, response.confidence());
        assertEquals("v2", response.parserVersion());
        assertEquals("trace-1", response.ingestionTraceId());
    }
}
