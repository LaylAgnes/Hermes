package com.hermes.jobs.search;

import com.hermes.jobs.job.JobEntity;
import com.hermes.jobs.job.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final JobRepository repository;

    public Page<JobEntity> search(String query, Pageable pageable) {

        SearchCriteria criteria = QueryParser.parse(query);

        List<JobEntity> all = repository.findByActiveTrue(pageable).getContent();

        List<JobEntity> ranked = all.stream()
                .sorted(Comparator.comparingInt(job -> -score(job, criteria)))
                .toList();

        return new PageImpl<>(ranked, pageable, ranked.size());
    }

    private int score(JobEntity job, SearchCriteria c) {

        int score = 0;

        String text = (job.getUrl() + " " + job.getEmpresa()).toLowerCase();

        for (String stack : c.stacks)
            if (text.contains(stack))
                score += 5;

        for (var area : c.areas)
            if (text.contains(area.name().toLowerCase()))
                score += 4;

        for (var s : c.seniorities)
            if (text.contains(s.name().toLowerCase()))
                score += 6;

        if (c.remote && text.contains("remote"))
            score += 3;

        if (c.country != null && text.contains(c.country))
            score += 3;

        return score;
    }
}