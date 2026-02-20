package com.hermes.jobs.job.mapper;

import com.hermes.jobs.job.JobEntity;
import com.hermes.jobs.job.dto.JobResponse;

public class JobMapper {

    public static JobResponse toResponse(JobEntity e) {
        return new JobResponse(
                e.getId(),
                e.getUrl(),
                e.getEmpresa(),
                e.getDomain(),
                e.getSource(),
                e.getTitle(),
                e.getLocation(),
                e.getDescription(),
                e.getStacks(),
                e.getSeniority(),
                e.getWorkMode(),
                e.getColetadoEm(),
                e.isActive()
        );
    }
}
