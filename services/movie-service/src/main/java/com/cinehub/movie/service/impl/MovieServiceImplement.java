package com.cinehub.movie.service.impl;

import com.cinehub.movie.service.MovieService;
import com.cinehub.movie.dto.MovieDetailResponse;
import com.cinehub.movie.dto.MovieSummaryResponse;
import com.cinehub.movie.dto.TMDb.TMDbCreditsResponse;
import com.cinehub.movie.dto.TMDb.TMDbMovieResponse;
import com.cinehub.movie.dto.TMDb.TMDbReleaseDatesResponse;
import com.cinehub.movie.entity.MovieDetail;
import com.cinehub.movie.entity.MovieStatus;
import com.cinehub.movie.entity.MovieSummary;
import com.cinehub.movie.mapper.MovieMapper;
import com.cinehub.movie.dto.response.PagedResponse;
import com.cinehub.movie.repository.MovieDetailRepository;
import com.cinehub.movie.repository.MovieSummaryRepository;
import com.cinehub.movie.service.client.TMDbClient;
import com.cinehub.movie.util.AgeRatingNormalizer;
import com.cinehub.movie.util.RegexUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j

public class MovieServiceImplement implements MovieService {

    private final MovieSummaryRepository movieSummaryRepository;
    private final MovieDetailRepository movieDetailRepository;
    private final TMDbClient tmdbClient;
    private final MovieMapper movieMapper;
    private final MongoTemplate mongoTemplate;

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

        List<TMDbMovieResponse> allMovies = new ArrayList<>();
        allMovies.addAll(nowPlaying);
        allMovies.addAll(upcoming);

        Set<Integer> activeTmdbIds = allMovies.stream().map(TMDbMovieResponse::getId).collect(Collectors.toSet());

        for (TMDbMovieResponse movie : nowPlaying) {
            TMDbMovieResponse fullMovie = tmdbClient.fetchMovieDetail(movie.getId());
            syncMovie(fullMovie, MovieStatus.NOW_PLAYING);
        }
        for (TMDbMovieResponse movie : upcoming) {
            TMDbMovieResponse fullMovie = tmdbClient.fetchMovieDetail(movie.getId());
            syncMovie(fullMovie, MovieStatus.UPCOMING);
        }

        List<MovieSummary> dbMovies = movieSummaryRepository.findAll();
        for (MovieSummary summary : dbMovies) {
            if (!activeTmdbIds.contains(summary.getTmdbId())) {
                if (summary.getStatus() != MovieStatus.ARCHIVED) {
                    summary.setStatus(MovieStatus.ARCHIVED);
                    movieSummaryRepository.save(summary);
                    log.info("Archived movie with tmdb={}", summary.getTmdbId());
                }
            }
        }

        log.info("[{}] Movie sync completed. {} movies active.",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                activeTmdbIds.size());
    }

    private void syncMovie(TMDbMovieResponse movie, MovieStatus status) {

        TMDbCreditsResponse credits = tmdbClient.fetchCredits(movie.getId());
        TMDbReleaseDatesResponse releaseDates = tmdbClient.fetchReleaseDates(movie.getId());
        String trailer = tmdbClient.fetchTrailerKey(movie.getId());
        String age = AgeRatingNormalizer.normalize(extractAgeRating(releaseDates));

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

    private String extractAgeRating(TMDbReleaseDatesResponse releaseDates) {
        return releaseDates.getResults().stream()
                .filter(r -> "US".equals(r.getIso31661())) // ưu tiên US
                .flatMap(r -> r.getReleaseDates().stream())
                .map(TMDbReleaseDatesResponse.ReleaseDate::getCertification)
                .filter(c -> c != null && !c.isEmpty())
                .findFirst()
                .orElse(null);
    }

    public Page<MovieSummaryResponse> getNowPlayingMovies(Pageable pageable) {
        Page<MovieSummary> entities = movieSummaryRepository.findByStatus(MovieStatus.NOW_PLAYING, pageable);
        return movieMapper.toSummaryResponsePage(entities);
    }

    public Page<MovieSummaryResponse> getUpcomingMovies(Pageable pageable) {
        Page<MovieSummary> entities = movieSummaryRepository.findByStatus(MovieStatus.UPCOMING, pageable);
        return movieMapper.toSummaryResponsePage(entities);
    }

    public Page<MovieSummaryResponse> getArchivedMovies(Pageable pageable) {
        Page<MovieSummary> entities = movieSummaryRepository.findByStatus(MovieStatus.ARCHIVED, pageable);
        return movieMapper.toSummaryResponsePage(entities);
    }

    @Override
    public List<MovieSummaryResponse> searchMovies(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title parameter is required");
        }

        List<MovieSummary> entities = movieSummaryRepository
                .findByStatusNotAndTitleContainingIgnoreCase(MovieStatus.ARCHIVED, keyword);

        return entities.stream()
                .map(movieMapper::toSummaryResponse)
                .toList();
    }

    public MovieDetailResponse getMovieDetail(Integer tmdbId) {
        if (tmdbId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TMDb ID is required");
        }

        Optional<MovieDetail> movieDetail = movieDetailRepository.findByTmdbId(tmdbId);
        if (movieDetail.isPresent()) {
            return movieMapper.toDetailResponse(movieDetail.get());
        }

        // Không tìm thấy trong DB, gọi TMDb API
        try {
            log.info("Movie detail not found in DB for tmdbId={}. Fetching from TMDb API...", tmdbId);

            TMDbMovieResponse movieResponse = tmdbClient.fetchMovieDetail(tmdbId);
            if (movieResponse == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Movie not found with TMDb ID: " + tmdbId);
            }

            TMDbCreditsResponse credits = tmdbClient.fetchCredits(tmdbId);
            TMDbReleaseDatesResponse releaseDates = tmdbClient.fetchReleaseDates(tmdbId);
            String trailer = tmdbClient.fetchTrailerKey(tmdbId);
            String age = AgeRatingNormalizer.normalize(extractAgeRating(releaseDates));

            MovieDetail detail = new MovieDetail();

            detail.setId(UUID.randomUUID());
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

            // Lưu vào database

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

    @Transactional
    public MovieDetailResponse updateMovie(UUID id, MovieDetailResponse request) {
        MovieDetail existingDetail = movieDetailRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));

        MovieSummary existingSummary = movieSummaryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie summary not found"));

        if (request.getTitle() != null) {
            existingDetail.setTitle(request.getTitle());
            existingSummary.setTitle(request.getTitle());
        }

        if (request.getOverview() != null)
            existingDetail.setOverview(request.getOverview());

        if (request.getPosterUrl() != null)
            existingSummary.setPosterUrl(request.getPosterUrl());

        if (request.getGenres() != null && !request.getGenres().isEmpty()) {
            existingDetail.setGenres(request.getGenres());
            existingSummary.setGenres(request.getGenres());
        }

        if (request.getTime() != null)
            existingDetail.setTime(request.getTime());

        if (request.getCountry() != null)
            existingDetail.setCountry(request.getCountry());

        if (request.getTrailer() != null)
            existingDetail.setTrailer(request.getTrailer());

        if (request.getAge() != null)
            existingDetail.setAge(request.getAge());

        movieDetailRepository.save(existingDetail);
        movieSummaryRepository.save(existingSummary);

        return movieMapper.toDetailResponse(existingDetail);
    }

    @Override
    public PagedResponse<MovieSummaryResponse> adminSearch(String q, MovieStatus status, Pageable pageable) {
        Query query = new Query();

        if (q != null && !q.isBlank()) {
            query.addCriteria(Criteria.where("title").regex(".*" + RegexUtil.escape(q.trim()) + ".*", "i"));
        }

        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        // total count for the query (before paging)
        long total = mongoTemplate.count(query, MovieSummary.class);

        // apply paging & sort to the query
        query.with(pageable);

        // fetch paged content
        List<MovieSummary> list = mongoTemplate.find(query, MovieSummary.class);
        Page<MovieSummary> entityPage = new PageImpl<>(list, pageable, total);

        // convert to DTO page (reuse your mapper)
        Page<MovieSummaryResponse> dtoPage = movieMapper.toSummaryResponsePage(entityPage);

        // build and return PagedResponse
        return PagedResponse.<MovieSummaryResponse>builder()
                .data(dtoPage.getContent())
                .page(dtoPage.getNumber())
                .size(dtoPage.getSize())
                .totalElements(dtoPage.getTotalElements())
                .totalPages(dtoPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public void changeStatus(UUID id, MovieStatus status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is required");
        }

        MovieSummary summary = movieSummaryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));

        summary.setStatus(status);
        movieSummaryRepository.save(summary);
    }

    @Transactional
    public void deleteMovie(UUID id) {
        MovieDetail existingDetail = movieDetailRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));
        MovieSummary existingSummary = movieSummaryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie summary not found"));

        movieDetailRepository.delete(existingDetail);
        movieSummaryRepository.delete(existingSummary);

        log.info("Deleted movie manually: {}", existingDetail.getTitle());
    }
}
