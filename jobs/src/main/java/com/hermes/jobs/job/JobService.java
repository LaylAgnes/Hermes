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

        Set<String> uniqueUrls = new HashSet<>(request.urls());
        OffsetDateTime now = OffsetDateTime.now();

        List<JobEntity> toSave = new ArrayList<>();

        for (String url : uniqueUrls) {

            Optional<JobEntity> existingOpt = repository.findByUrl(url);

            if (existingOpt.isPresent()) {
                JobEntity entity = existingOpt.get();
                entity.setColetadoEm(now);
                entity.setActive(true);
                toSave.add(entity);
            } else {
                toSave.add(JobEntity.builder()
                        .url(url)
                        .empresa(UrlUtils.extractCompany(url))
                        .domain(UrlUtils.extractDomain(url))
                        .source(UrlUtils.extractSource(url))
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

            JobEntity entity = repository.findByUrl(doc.url())
                    .orElseGet(JobEntity::new);

            entity.setUrl(doc.url());
            entity.setEmpresa(UrlUtils.extractCompany(doc.url()));
            entity.setDomain(UrlUtils.extractDomain(doc.url()));
            entity.setSource(UrlUtils.extractSource(doc.url()));

            if (doc.title() != null) entity.setTitle(doc.title());
            if (doc.location() != null) entity.setLocation(doc.location());

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
            if (!result.stacks().isEmpty()) {
                String stacks = result.stacks().stream()
                        .sorted()
                        .collect(Collectors.joining(","));
                entity.setStacks(stacks);
            }

            if (result.seniority() != null)
                entity.setSeniority(result.seniority());

            if (result.workMode() != null)
                entity.setWorkMode(result.workMode());

            entity.setColetadoEm(now);
            entity.setActive(true);

            toSave.add(entity);
        }

        repository.saveAll(toSave);
        return toSave.size();
    }
}