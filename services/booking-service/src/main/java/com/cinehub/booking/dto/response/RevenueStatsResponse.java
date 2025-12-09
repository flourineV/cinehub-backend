package com.cinehub.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueStatsResponse {
    private int year;
    private Integer month;
    private BigDecimal totalRevenue;
    private long totalBookings;
    private BigDecimal averageOrderValue;
}
