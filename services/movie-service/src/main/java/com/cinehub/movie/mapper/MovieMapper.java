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
            entity.getSpokenLanguages(), // Trả về full list
            entity.getGenres()
        );
    }

    public Page<MovieSummaryResponse> toSummaryResponsePage(Page<MovieSummary> entityPage) {
        List<MovieSummaryResponse> dtos = entityPage.getContent().stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, entityPage.getPageable(), entityPage.getTotalElements());
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
            entity.getPosterUrl(),
            entity.getGenres(),
            entity.getTime(),
            entity.getCountry(),
            entity.getSpokenLanguages(), // Trả về full list
            entity.getCrew(),
            entity.getCast(),
            entity.getReleaseDate(),
            entity.getOverview(),
            entity.getTrailer()
        );
    }
}