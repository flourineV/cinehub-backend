package com.cinehub.movie.service;

import com.cinehub.movie.dto.MovieDetailResponse;
import com.cinehub.movie.dto.MovieSummaryResponse;
import com.cinehub.movie.dto.TMDb.TMDbCreditsResponse;
import com.cinehub.movie.dto.TMDb.TMDbMovieResponse;
import com.cinehub.movie.dto.TMDb.TMDbReleaseDatesResponse;
import com.cinehub.movie.entity.MovieDetail;
import com.cinehub.movie.entity.MovieSummary;
import com.cinehub.movie.mapper.MovieMapper;
import com.cinehub.movie.repository.MovieDetailRepository;
import com.cinehub.movie.repository.MovieSummaryRepository;
import com.cinehub.movie.service.client.TMDbClient;
import com.cinehub.movie.util.AgeRatingNormalizer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j

public class MovieService {

    private final MovieSummaryRepository movieSummaryRepository;
    private final MovieDetailRepository movieDetailRepository;
    private final TMDbClient tmdbClient; // client gọi TMDb API
    private final MovieMapper movieMapper;

    public MovieDetailResponse getMovieByUuid(UUID id) {
        MovieDetail entity = movieDetailRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Movie not found with UUID: " + id));

        return movieMapper.toDetailResponse(entity);
    }

    public void syncMovies() {
        log.info("[{}] Starting movie sync from TMDb...",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Lấy dữ liệu từ TMDb
        List<TMDbMovieResponse> nowPlaying = tmdbClient.fetchNowPlaying();
        List<TMDbMovieResponse> upcoming = tmdbClient.fetchUpcoming();

        // Merge lại 2 list
        List<TMDbMovieResponse> allMovies = new ArrayList<>();
        allMovies.addAll(nowPlaying);
        allMovies.addAll(upcoming);

        Set<Integer> activeTmdbIds = allMovies.stream().map(TMDbMovieResponse::getId).collect(Collectors.toSet());

        for (TMDbMovieResponse movie : nowPlaying) {
            TMDbMovieResponse fullMovie = tmdbClient.fetchMovieDetail(movie.getId());
            syncMovie(fullMovie, "NOW_PLAYING");
        }
        for (TMDbMovieResponse movie : upcoming) {
            TMDbMovieResponse fullMovie = tmdbClient.fetchMovieDetail(movie.getId());
            syncMovie(fullMovie, "UPCOMING");
        }

        // Xóa phim k còn active
        List<MovieSummary> dbMovies = movieSummaryRepository.findAll();
        for (MovieSummary summary : dbMovies) {
            if (!activeTmdbIds.contains(summary.getTmdbId())) {
                movieSummaryRepository.deleteByTmdbId(summary.getTmdbId());
                movieDetailRepository.deleteByTmdbId(summary.getTmdbId());
                log.info("Deleted movie with tmdb={}", summary.getTmdbId());
            }
        }
        log.info("[{}] Movie sync completed. {} movies active.",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                activeTmdbIds.size());
    }

    private void syncMovie(TMDbMovieResponse movie, String status) {
        // Gọi thêm API phụ
        TMDbCreditsResponse credits = tmdbClient.fetchCredits(movie.getId());
        TMDbReleaseDatesResponse releaseDates = tmdbClient.fetchReleaseDates(movie.getId());
        String trailer = tmdbClient.fetchTrailerKey(movie.getId());

        // Lấy age rating từ release_dates
        String age = AgeRatingNormalizer.normalize(extractAgeRating(releaseDates));

        // --- Summary ---
        MovieSummary summary = movieSummaryRepository.findByTmdbId(movie.getId())
                .orElse(new MovieSummary());

        UUID sharedId = summary.getId() != null ? summary.getId() : UUID.randomUUID();

        summary.setId(sharedId);
        summary.setTmdbId(movie.getId());
        summary.setTitle(movie.getTitle());
        summary.setPosterUrl(movie.getPosterPath());
        summary.setStatus(status);
        summary.setSpokenLanguages(
                movie.getSpokenLanguages().stream()
                        .map(TMDbMovieResponse.SpokenLanguage::getIso6391)
                        .toList());
        summary.setCountry(
                movie.getProductionCountries().isEmpty() ? null : movie.getProductionCountries().get(0).getName());
        summary.setTime(movie.getRuntime());
        summary.setGenres(movie.getGenres().stream().map(TMDbMovieResponse.Genre::getName).toList());
        summary.setAge(age);
        summary.setTrailer(trailer);

        movieSummaryRepository.save(summary);

        // --- Detail ---
        MovieDetail detail = movieDetailRepository.findByTmdbId(movie.getId())
                .orElse(new MovieDetail());

        detail.setId(sharedId);
        detail.setTmdbId(movie.getId());
        detail.setTitle(movie.getTitle());
        detail.setOverview(movie.getOverview());
        detail.setTime(movie.getRuntime());
        detail.setSpokenLanguages(
                movie.getSpokenLanguages().stream()
                        .map(TMDbMovieResponse.SpokenLanguage::getEnglishName)
                        .toList());
        detail.setCountry(
                movie.getProductionCountries().isEmpty() ? null : movie.getProductionCountries().get(0).getName());
        detail.setReleaseDate(movie.getReleaseDate());
        detail.setGenres(movie.getGenres().stream().map(TMDbMovieResponse.Genre::getName).toList());
        detail.setCast(
                credits.getCast().stream().map(TMDbCreditsResponse.Cast::getName).limit(10).toList());
        detail.setCrew(
                credits.getCrew().stream()
                        .filter(c -> "Director".equalsIgnoreCase(c.getJob()))
                        .map(TMDbCreditsResponse.Crew::getName)
                        .toList());
        detail.setAge(age);
        detail.setTrailer(trailer);
        detail.setPosterUrl(movie.getPosterPath());

        movieDetailRepository.save(detail);

        log.info("Synced movie: {} ({})", movie.getTitle(), movie.getId());
    }

    // HelperL extract age
    private String extractAgeRating(TMDbReleaseDatesResponse releaseDates) {
        return releaseDates.getResults().stream()
                .filter(r -> "US".equals(r.getIso31661())) // ưu tiên US
                .flatMap(r -> r.getReleaseDates().stream())
                .map(TMDbReleaseDatesResponse.ReleaseDate::getCertification)
                .filter(c -> c != null && !c.isEmpty())
                .findFirst()
                .orElse(null);
    }

    // ================== PUBLIC API METHODS ==================

    public Page<MovieSummaryResponse> getNowPlayingMovies(Pageable pageable) {
        Page<MovieSummary> entities = movieSummaryRepository.findByStatus("NOW_PLAYING", pageable);
        return movieMapper.toSummaryResponsePage(entities);
    }

    public Page<MovieSummaryResponse> getUpcomingMovies(Pageable pageable) {
        Page<MovieSummary> entities = movieSummaryRepository.findByStatus("UPCOMING", pageable);
        return movieMapper.toSummaryResponsePage(entities);
    }

    public Page<MovieSummaryResponse> searchMovies(String title, Pageable pageable) {
        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title parameter is required");
        }
        Page<MovieSummary> entities = movieSummaryRepository.findByTitleContainingIgnoreCase(title.trim(), pageable);
        return movieMapper.toSummaryResponsePage(entities);
    }

    public MovieDetailResponse getMovieDetail(Integer tmdbId) {
        if (tmdbId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TMDb ID is required");
        }

        // Tìm trong database trước
        Optional<MovieDetail> movieDetail = movieDetailRepository.findByTmdbId(tmdbId);
        if (movieDetail.isPresent()) {
            return movieMapper.toDetailResponse(movieDetail.get());
        }

        // Nếu không tìm thấy trong DB, gọi TMDb API
        try {
            log.info("Movie detail not found in DB for tmdbId={}. Fetching from TMDb API...", tmdbId);

            TMDbMovieResponse movieResponse = tmdbClient.fetchMovieDetail(tmdbId);
            if (movieResponse == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Movie not found with TMDb ID: " + tmdbId);
            }

            // Fetch additional data for complete movie detail
            TMDbCreditsResponse credits = tmdbClient.fetchCredits(tmdbId);
            TMDbReleaseDatesResponse releaseDates = tmdbClient.fetchReleaseDates(tmdbId);
            String trailer = tmdbClient.fetchTrailerKey(tmdbId);
            String age = AgeRatingNormalizer.normalize(extractAgeRating(releaseDates));

            // Tạo MovieDetail từ TMDb data
            MovieDetail detail = new MovieDetail();

            detail.setId(UUID.randomUUID()); // <--- Thêm đoạn này

            detail.setTmdbId(movieResponse.getId());
            detail.setTitle(movieResponse.getTitle());
            detail.setOverview(movieResponse.getOverview());
            detail.setTime(movieResponse.getRuntime());
            detail.setSpokenLanguages(
                    movieResponse.getSpokenLanguages().stream()
                            .map(TMDbMovieResponse.SpokenLanguage::getEnglishName)
                            .toList());
            detail.setCountry(
                    movieResponse.getProductionCountries().isEmpty() ? null
                            : movieResponse.getProductionCountries().get(0).getName());
            detail.setReleaseDate(movieResponse.getReleaseDate());
            detail.setGenres(movieResponse.getGenres().stream().map(TMDbMovieResponse.Genre::getName).toList());
            detail.setCast(
                    credits.getCast().stream().map(TMDbCreditsResponse.Cast::getName).limit(10).toList());
            detail.setCrew(
                    credits.getCrew().stream()
                            .filter(c -> "Director".equalsIgnoreCase(c.getJob()))
                            .map(TMDbCreditsResponse.Crew::getName)
                            .toList());
            detail.setAge(age);
            detail.setTrailer(trailer);

            // Lưu vào database để lần sau không phải gọi API
            movieDetailRepository.save(detail);
            log.info("Saved movie detail from TMDb API: {} ({})", movieResponse.getTitle(), tmdbId);

            return movieMapper.toDetailResponse(detail);

        } catch (Exception e) {
            log.error("Error fetching movie detail from TMDb API for tmdbId={}: {}", tmdbId, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Movie not found with TMDb ID: " + tmdbId);
        }
    }
}
