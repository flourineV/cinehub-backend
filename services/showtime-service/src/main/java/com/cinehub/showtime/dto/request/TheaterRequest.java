package com.cinehub.showtime.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder

public class TheaterRequest {
    private UUID provinceId;
    private String name;
    private String address;
    private String description;
}
