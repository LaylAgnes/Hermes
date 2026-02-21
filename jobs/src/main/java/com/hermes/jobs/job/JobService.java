package com.hermes.jobs.job;

import com.hermes.jobs.UrlUtils;
import com.hermes.jobs.job.dto.ImportRequest;
import com.hermes.jobs.job.dto.JobImportRequest;
import com.hermes.jobs.search.JobClassifier;
import com.hermes.jobs.search.TextSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository repository;

    // =========================
    // IMPORTAÇÃO (INDEXADOR DE URL)
    // =========================
    @Transactional
    public int importUrls(ImportRequest request) {

        if (request.urls() == null || request.urls().isEmpty())
            return 0;

        Set<String> uniqueUrls = request.urls().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .filter(this::isSupportedUrl)
                .collect(Collectors.toSet());

        if (uniqueUrls.isEmpty())
            return 0;

        OffsetDateTime now = OffsetDateTime.now();

        List<JobEntity> toSave = new ArrayList<>();

        for (String url : uniqueUrls) {

            Optional<JobEntity> existingOpt = repository.findByUrl(url);

            if (existingOpt.isPresent()) {
                JobEntity entity = existingOpt.get();
                entity.setColetadoEm(now);
                entity.setActive(true);
                if (entity.getSourceType() == null) entity.setSourceType("url-index");
                if (entity.getSourceName() == null) entity.setSourceName(entity.getDomain());
                if (entity.getConfidence() == null) entity.setConfidence(0.5);
                if (entity.getParserVersion() == null) entity.setParserVersion("url-index-v1");
                if (entity.getIngestionTraceId() == null) entity.setIngestionTraceId(java.util.UUID.randomUUID().toString());
                toSave.add(entity);
            } else {
                toSave.add(JobEntity.builder()
                        .url(url)
                        .empresa(UrlUtils.extractCompany(url))
                        .domain(UrlUtils.extractDomain(url))
                        .source(UrlUtils.extractSource(url))
                        .sourceType("url-index")
                        .sourceName(UrlUtils.extractDomain(url))
                        .confidence(0.5)
                        .parserVersion("url-index-v1")
                        .ingestionTraceId(java.util.UUID.randomUUID().toString())
                        .coletadoEm(now)
                        .active(true)
                        .build());
            }
        }

        repository.saveAll(toSave);
        return toSave.size();
    }

    // =========================
    // CONSULTAS
    // =========================

    public Page<JobEntity> list(Pageable pageable) {
        return repository.findByActiveTrue(pageable);
    }

    public Page<JobEntity> byDomain(String domain, Pageable pageable) {
        return repository.findByDomainAndActiveTrue(domain, pageable);
    }

    public Page<JobEntity> search(String q, Pageable pageable) {
        return repository.findByEmpresaContainingIgnoreCaseAndActiveTrue(q, pageable);
    }

    public Page<JobEntity> bySource(String source, Pageable pageable) {
        return repository.findBySourceAndActiveTrue(source, pageable);
    }

    // =========================
    // IMPORTAÇÃO COMPLETA (CRAWLER)
    // =========================
    @Transactional
    public int importDocuments(JobImportRequest request) {

        if (request.jobs() == null || request.jobs().isEmpty())
            return 0;

        OffsetDateTime now = OffsetDateTime.now();
        List<JobEntity> toSave = new ArrayList<>();

        for (var doc : request.jobs()) {
            if (doc == null || doc.url() == null || doc.url().isBlank())
                continue;

            String normalizedUrl = doc.url().trim();
            if (!isSupportedUrl(normalizedUrl))
                continue;

            JobEntity entity = repository.findByUrl(normalizedUrl)
                    .orElseGet(JobEntity::new);

            entity.setUrl(normalizedUrl);
            entity.setEmpresa(UrlUtils.extractCompany(normalizedUrl));
            entity.setDomain(UrlUtils.extractDomain(normalizedUrl));
            entity.setSource(UrlUtils.extractSource(normalizedUrl));
            entity.setSourceType(normalizeNullableText(doc.sourceType()));
            entity.setSourceName(normalizeNullableText(doc.sourceName()));
            entity.setConfidence(doc.confidence() == null ? 0.0 : Math.max(0.0, Math.min(1.0, doc.confidence())));
            entity.setParserVersion(normalizeNullableText(doc.parserVersion()));
            entity.setIngestionTraceId(normalizeNullableText(doc.ingestionTraceId()));

            entity.setTitle(normalizeNullableText(doc.title()));
            entity.setLocation(normalizeNullableText(doc.location()));

            // limpa texto
            String clean = TextSanitizer.clean(doc.description());
            entity.setDescription(clean);

            // ================= CLASSIFICAÇÃO =================
            String classificationText =
                    (entity.getTitle() == null ? "" : entity.getTitle() + " ") +
                            (entity.getLocation() == null ? "" : entity.getLocation() + " ") +
                            clean;

            var result = JobClassifier.classify(classificationText);

            // stacks ordenados para consistência
            entity.setStacks(result.stacks().isEmpty()
                    ? null
                    : result.stacks().stream().sorted().collect(Collectors.joining(",")));

            entity.setSeniority(result.seniority());
            entity.setWorkMode(result.workMode());

            if (entity.getSourceType() == null) entity.setSourceType(entity.getSource());
            if (entity.getSourceName() == null) entity.setSourceName(entity.getEmpresa());
            if (entity.getParserVersion() == null) entity.setParserVersion("unknown");
            if (entity.getIngestionTraceId() == null) entity.setIngestionTraceId(java.util.UUID.randomUUID().toString());

            entity.setColetadoEm(now);
            entity.setActive(true);

            toSave.add(entity);
        }

        if (toSave.isEmpty())
            return 0;

        repository.saveAll(toSave);
        return toSave.size();
    }

    private boolean isSupportedUrl(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getHost() != null
                    && uri.getScheme() != null
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
        } catch (Exception ignored) {
            return false;
        }
    }

    private String normalizeNullableText(String value) {
        if (value == null)
            return null;

        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
