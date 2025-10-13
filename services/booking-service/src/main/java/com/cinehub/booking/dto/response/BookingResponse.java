package com.cinehub.booking.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private UUID bookingId;
    private UUID userId;
    private UUID showtimeId;
    private BigDecimal totalPrice;
    private String status;
    private List<BookingSeatResponse> seats;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
