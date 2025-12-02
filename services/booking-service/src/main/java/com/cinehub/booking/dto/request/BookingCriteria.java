package com.cinehub.booking.dto.request;

import com.cinehub.booking.entity.BookingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class BookingCriteria {
    private String keyword; // Partial match: userId, showtimeId, bookingCode, guestName
    private UUID userId;
    private List<UUID> userIds; // For username search - multiple matching users
    private String username; // Partial match on user's fullName from user-profile-service
    private UUID showtimeId;
    private UUID movieId;
    private String bookingCode;
    private BookingStatus status;
    private String paymentMethod;
    private String guestName;
    private String guestEmail;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}