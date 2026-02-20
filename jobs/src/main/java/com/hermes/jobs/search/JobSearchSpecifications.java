package com.hermes.jobs.search;

import com.hermes.jobs.job.JobEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class JobSearchSpecifications {

    private JobSearchSpecifications() {
    }

    public static Specification<JobEntity> from(SearchCriteria criteria) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("active")));

            if (!criteria.workModes.isEmpty()) {
                List<jakarta.persistence.criteria.Predicate> workModePredicates = criteria.workModes.stream()
                        .map(mode -> cb.like(cb.lower(cb.coalesce(root.get("workMode"), "")), "%" + mode + "%"))
                        .toList();
                predicates.add(cb.or(workModePredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
            }

            if (!criteria.seniorities.isEmpty()) {
                List<jakarta.persistence.criteria.Predicate> seniorityPredicates = criteria.seniorities.stream()
                        .map(level -> cb.like(cb.lower(cb.coalesce(root.get("seniority"), "")), "%" + level.name().toLowerCase() + "%"))
                        .toList();
                predicates.add(cb.or(seniorityPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
            }

            if (!criteria.stacks.isEmpty()) {
                List<jakarta.persistence.criteria.Predicate> stackPredicates = criteria.stacks.stream()
                        .map(stack -> cb.like(cb.lower(cb.coalesce(root.get("stacks"), "")), "%" + stack + "%"))
                        .toList();
                predicates.add(cb.or(stackPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
            }

            if (!criteria.locationTerms.isEmpty()) {
                List<jakarta.persistence.criteria.Predicate> locationPredicates = criteria.locationTerms.stream()
                        .map(location -> cb.like(cb.lower(cb.coalesce(root.get("location"), "")), "%" + location + "%"))
                        .toList();
                predicates.add(cb.or(locationPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
            }

            if (!criteria.freeTextTerms.isEmpty()) {
                for (String token : criteria.freeTextTerms) {
                    String likeToken = "%" + token + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(cb.coalesce(root.get("title"), "")), likeToken),
                            cb.like(cb.lower(cb.coalesce(root.get("description"), "")), likeToken),
                            cb.like(cb.lower(cb.coalesce(root.get("empresa"), "")), likeToken),
                            cb.like(cb.lower(cb.coalesce(root.get("stacks"), "")), likeToken)
                    ));
                }
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
