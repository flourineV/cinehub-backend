package com.cinehub.showtime.dto.request;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BatchInitializeSeatsRequest {
    private List<UUID> showtimeIds;
}
