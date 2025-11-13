package com.cinehub.movie.mapper;

import com.cinehub.movie.dto.response.MovieStatsResponse;
import com.cinehub.movie.dto.response.MovieMonthlyStatsResponse;
import org.springframework.stereotype.Component;

@Component
public class MovieStatsMapper {

    public MovieStatsResponse toOverview(long total, long nowPlaying, long upcoming, long archived) {
        MovieStatsResponse dto = new MovieStatsResponse();
        dto.setTotalMovies(total);
        dto.setNowPlaying(nowPlaying);
        dto.setUpcoming(upcoming);
        dto.setArchived(archived);
        return dto;
    }

    public MovieMonthlyStatsResponse toMonthly(Object[] row) {
        MovieMonthlyStatsResponse dto = new MovieMonthlyStatsResponse();
        dto.setYear(((Number) row[0]).intValue());
        dto.setMonth(((Number) row[1]).intValue());
        dto.setAddedMovies(((Number) row[2]).longValue());
        return dto;
    }
}
