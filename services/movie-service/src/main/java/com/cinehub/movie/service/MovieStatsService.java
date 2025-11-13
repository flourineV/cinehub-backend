package com.cinehub.movie.service;

import com.cinehub.movie.dto.response.MovieStatsResponse;
import com.cinehub.movie.dto.response.MovieMonthlyStatsResponse;
import com.cinehub.movie.entity.MovieStatus;
import com.cinehub.movie.mapper.MovieStatsMapper;
import com.cinehub.movie.repository.MovieSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieStatsService {

    private final MovieSummaryRepository movieSummaryRepository;
    private final MovieStatsMapper movieStatsMapper;

    public MovieStatsResponse getOverview() {
        long total = movieSummaryRepository.count();
        long nowPlaying = movieSummaryRepository.countByStatus(MovieStatus.NOW_PLAYING);
        long upcoming = movieSummaryRepository.countByStatus(MovieStatus.UPCOMING);
        long archived = movieSummaryRepository.countByStatus(MovieStatus.ARCHIVED);

        return movieStatsMapper.toOverview(total, nowPlaying, upcoming, archived);
    }

    public List<MovieMonthlyStatsResponse> getMonthlyStats() {
        return movieSummaryRepository.countMoviesAddedByMonth().stream()
                .map(movieStatsMapper::toMonthly)
                .collect(Collectors.toList());
    }
}
