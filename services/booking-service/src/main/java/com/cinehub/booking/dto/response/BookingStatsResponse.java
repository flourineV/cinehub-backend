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
public class BookingStatsResponse {
    private long totalBookings;
    private long confirmedBookings;
    private long cancelledBookings;
    private long refundedBookings;
    private long pendingBookings;
    private BigDecimal totalRevenue;
}
