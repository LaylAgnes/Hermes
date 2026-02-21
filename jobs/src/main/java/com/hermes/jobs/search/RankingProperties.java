package com.hermes.jobs.search;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "search.ranking")
public class RankingProperties {

    private boolean featureLoggingEnabled = false;
    private double weightHeuristic = 1.0;
    private double weightTitleHits = 1.3;
    private double weightDescriptionHits = 0.7;
    private double weightStackHits = 1.2;
    private double weightSeniorityMatch = 1.0;
    private double weightFreshnessDays = 0.2;
}
