package com.cinehub.booking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateBookingRequest {

    private UUID showtimeId;

    private List<SeatSelectionDetail> selectedSeats;

    private String guestName;
    private String guestEmail;

    private UUID userId;
    private UUID guestSessionId;
}
