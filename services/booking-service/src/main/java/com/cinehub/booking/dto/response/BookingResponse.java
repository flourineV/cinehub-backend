package com.cinehub.booking.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private UUID bookingId;
    private String bookingCode;
    private UUID userId;
    private String fullName; // From user-profile-service
    private UUID showtimeId;
    private UUID movieId; // Add movieId for batch lookup
    private String movieTitle; // From movie-service
    private String guestName;
    private String guestEmail;
    private String status;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private String paymentMethod;
    private String transactionId;
    private List<BookingSeatResponse> seats;
}