package com.cinehub.booking.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private String bookingId;
    private String userId;
    private String showtimeId;
    private BigDecimal totalPrice;
    private String status;
    private List<BookingSeatResponse> seats;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
