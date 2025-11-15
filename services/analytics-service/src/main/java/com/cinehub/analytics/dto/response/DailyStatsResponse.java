package com.cinehub.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyStatsResponse {

    private LocalDate date;
    private Long totalPageViews;
    private Long uniqueVisitors;
    private Long totalBookings;
    private Double totalRevenue;
    private Long newUsers;
    private Long guestUsers;
}
