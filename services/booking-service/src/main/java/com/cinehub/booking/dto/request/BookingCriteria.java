package com.cinehub.booking.dto.request;

import com.cinehub.booking.entity.BookingStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BookingCriteria {
    private UUID userId;
    private UUID showtimeId;
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