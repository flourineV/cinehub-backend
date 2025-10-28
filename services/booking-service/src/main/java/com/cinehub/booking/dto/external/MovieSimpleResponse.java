package com.cinehub.booking.dto.external;

import java.util.UUID;
import lombok.Data;

@Data
public class MovieSimpleResponse {
    private UUID id;
    private String title;
}
