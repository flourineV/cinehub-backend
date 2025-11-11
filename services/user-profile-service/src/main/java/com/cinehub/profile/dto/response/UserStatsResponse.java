package com.cinehub.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {

    private RankDistribution rankDistribution;
    private List<CinemaStaffCount> staffCounts;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CinemaStaffCount {
        private String cinemaId; // để service khác join lấy tên
        private long managerCount;
        private long staffCount;
    }
}
