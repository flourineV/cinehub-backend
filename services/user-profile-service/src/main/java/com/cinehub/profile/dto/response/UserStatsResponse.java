package com.cinehub.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {

    private RankDistribution rankDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankDistribution {
        private long bronzeCount;
        private long silverCount;
        private long goldCount;

        private double bronzePercentage;
        private double silverPercentage;
        private double goldPercentage;
    }
}
