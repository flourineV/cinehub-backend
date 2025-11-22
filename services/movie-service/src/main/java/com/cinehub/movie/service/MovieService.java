package com.cinehub.movie.service;

import com.cinehub.movie.dto.AddMovieFromTmdbRequest;
import com.cinehub.movie.dto.BulkAddMoviesRequest;
import com.cinehub.movie.dto.BulkAddMoviesResponse;
import com.cinehub.movie.dto.MovieDetailResponse;
import com.cinehub.movie.dto.MovieSummaryResponse;
import com.cinehub.movie.dto.response.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.cinehub.movie.entity.MovieStatus;

import java.time.LocalDate;
import java.util.UUID;
import java.util.List;

public interface MovieService {

    MovieDetailResponse getMovieByUuid(UUID id);

    void syncMovies();

    Page<MovieSummaryResponse> getNowPlayingMovies(Pageable pageable);

    Page<MovieSummaryResponse> getUpcomingMovies(Pageable pageable);

    Page<MovieSummaryResponse> getArchivedMovies(Pageable pageable);

    List<MovieSummaryResponse> searchMovies(String keyword);

    MovieDetailResponse getMovieDetail(Integer tmdbId);

    MovieDetailResponse updateMovie(UUID id, MovieDetailResponse request);

    PagedResponse<MovieSummaryResponse> adminSearch(String keyword, MovieStatus status, String genres, int page,
            int size,
            String sortBy, String sortType);

    void deleteMovie(UUID id);

    void changeStatus(UUID id, MovieStatus status);

    MovieDetailResponse addMovieFromTmdb(AddMovieFromTmdbRequest request);

    BulkAddMoviesResponse bulkAddMoviesFromTmdb(BulkAddMoviesRequest request);

    List<MovieSummaryResponse> getAvailableMoviesForDateRange(LocalDate startDate, LocalDate endDate);

    void setMovieNowPlaying(UUID id);
}
