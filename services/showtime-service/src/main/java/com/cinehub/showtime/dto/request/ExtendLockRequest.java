package com.cinehub.showtime.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtendLockRequest {
    private UUID showtimeId;
    private List<UUID> seatIds;
    private UUID userId;
    private UUID guestSessionId;
}
