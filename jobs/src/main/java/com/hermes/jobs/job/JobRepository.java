package com.hermes.jobs.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    Optional<JobEntity> findByUrl(String url);

    Page<JobEntity> findByActiveTrue(Pageable pageable);

    Page<JobEntity> findByDomainAndActiveTrue(String domain, Pageable pageable);

    Page<JobEntity> findByEmpresaContainingIgnoreCaseAndActiveTrue(String empresa, Pageable pageable);

    Page<JobEntity> findBySourceAndActiveTrue(String source, Pageable pageable);
}