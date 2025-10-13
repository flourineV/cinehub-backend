package com.cinehub.booking.dto.external;

import lombok.Data;
import java.util.UUID;

@Data
public class SeatSelectionDetail {
    private UUID seatId; // ID ghế
    private String seatType; // Loại ghế (NORMAL/VIP/COUPLE)
    private String ticketType; // Loại vé (ADULT/CHILD/STUDENT)
}