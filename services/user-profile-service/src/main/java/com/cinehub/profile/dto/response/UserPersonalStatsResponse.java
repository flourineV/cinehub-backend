package com.cinehub.profile.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPersonalStatsResponse {

    private Long totalBookings;
    private Long totalFavoriteMovies;
    private Integer loyaltyPoints;
}
