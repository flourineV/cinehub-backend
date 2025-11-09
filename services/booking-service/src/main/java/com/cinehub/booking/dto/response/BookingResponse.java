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
    private String guestName;
    private String guestEmail;

    private String status;

    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;

    private String paymentMethod;

    private String transactionId;

    private List<BookingSeatResponse> seats;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}