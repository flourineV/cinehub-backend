package com.cinehub.analytics.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackActivityRequest {

    private String activityType; // VIEW_MOVIE, SEARCH, BOOK_TICKET, etc.
    private String entityType; // MOVIE, SHOWTIME, BOOKING
    private UUID entityId;
    private String metadata; // JSON string
    private String sessionId;
}
