package com.cinehub.showtime.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder

public class TheaterResponse {
    private UUID id;
    private String name;
    private String nameEn;
    private String address;
    private String addressEn;
    private String description;
    private String descriptionEn;
    private String provinceName;
    private String provinceNameEn;
    private String imageUrl;
}
