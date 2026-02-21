package com.hermes.jobs.job;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_job_url", columnList = "url", unique = true),
        @Index(name = "idx_job_domain", columnList = "domain"),
        @Index(name = "idx_job_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 1000)
    private String url;

    @Column(nullable = false)
    private String empresa;

    @Column(nullable = false)
    private String domain;

    @Column(nullable = false)
    private OffsetDateTime coletadoEm;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private String source;

    @Column(length = 120)
    private String sourceType;

    @Column(length = 200)
    private String sourceName;

    @Column
    private Double confidence;

    @Column(length = 80)
    private String parserVersion;

    @Column(length = 150)
    private String ingestionTraceId;

    @Column(length = 500)
    private String title;

    @Column(length = 300)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String stacks;

    @Column(length = 200)
    private String seniority;

    @Column(length = 200)
    private String workMode;
}
