package com.hermes.jobs.search;

import com.hermes.jobs.job.JobEntity;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JobSearchSpecifications {

    private static final Map<Area, Set<String>> AREA_KEYWORDS = Map.of(
            Area.BACKEND, Set.of("backend", "back-end", "api", "microservices"),
            Area.FRONTEND, Set.of("frontend", "front-end", "ui", "ux"),
            Area.FULLSTACK, Set.of("fullstack", "full-stack"),
            Area.MOBILE, Set.of("mobile", "android", "ios", "react-native", "flutter"),
            Area.DATA, Set.of("data", "dados", "analytics", "machine learning"),
            Area.DEVOPS, Set.of("devops", "sre", "infra", "platform"),
            Area.SECURITY, Set.of("security", "seguranca", "cyber"),
            Area.QA, Set.of("qa", "quality", "test")
    );

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

            if (!criteria.areas.isEmpty()) {
                List<jakarta.persistence.criteria.Predicate> areaPredicates = criteria.areas.stream()
                        .map(area -> buildAreaPredicate(area, root, cb))
                        .toList();
                predicates.add(cb.or(areaPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
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

    private static jakarta.persistence.criteria.Predicate buildAreaPredicate(
            Area area,
            jakarta.persistence.criteria.Root<JobEntity> root,
            jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        Set<String> keywords = AREA_KEYWORDS.getOrDefault(area, Set.of(area.name().toLowerCase()));

        List<jakarta.persistence.criteria.Predicate> predicates = keywords.stream()
                .map(keyword -> {
                    String likeToken = "%" + keyword + "%";
                    return cb.or(
                            cb.like(cb.lower(cb.coalesce(root.get("title"), "")), likeToken),
                            cb.like(cb.lower(cb.coalesce(root.get("description"), "")), likeToken)
                    );
                })
                .toList();

        return cb.or(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
    }
}
