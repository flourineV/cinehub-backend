package com.cinehub.movie.mapper;

import com.cinehub.movie.dto.MovieDetailResponse;
import com.cinehub.movie.dto.MovieSummaryResponse;
import com.cinehub.movie.entity.MovieDetail;
import com.cinehub.movie.entity.MovieSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MovieMapper {

    public MovieSummaryResponse toSummaryResponse(MovieSummary entity) {
        if (entity == null) {
            return null;
        }

        return new MovieSummaryResponse(
                entity.getId(),
                entity.getTmdbId(),
                entity.getTitle(),
                entity.getPosterUrl(),
                entity.getAge(),
                entity.getStatus(),
                entity.getTime(),
                entity.getSpokenLanguages(),
                entity.getGenres(),
                entity.getTrailer(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getPopularity());
    }

    public Page<MovieSummaryResponse> toSummaryResponsePage(Page<MovieSummary> entityPage) {
        List<MovieSummaryResponse> dtos = entityPage.getContent().stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, entityPage.getPageable(), entityPage.getTotalElements());
    }

    public List<MovieSummaryResponse> toSummaryResponseList(List<MovieSummary> list) {
        return list.stream().map(this::toSummaryResponse).toList();
    }

    public MovieDetailResponse toDetailResponse(MovieDetail entity) {
        if (entity == null) {
            return null;
        }

        return new MovieDetailResponse(
                entity.getId(),
                entity.getTmdbId(),
                entity.getTitle(),
                entity.getAge(),
                entity.getStatus(),
                entity.getPosterUrl(),
                entity.getGenres(),
                entity.getTime(),
                entity.getCountry(),
                entity.getSpokenLanguages(),
                entity.getCrew(),
                entity.getCast(),
                entity.getReleaseDate(),
                entity.getOverview(),
                entity.getTrailer(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getPopularity());
    }
}