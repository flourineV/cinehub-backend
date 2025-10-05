package com.cinehub.profile.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankResponse {
    private UUID id;
    private String name;
    private Integer minPoints;
    private Integer maxPoints;
    private BigDecimal discountRate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
