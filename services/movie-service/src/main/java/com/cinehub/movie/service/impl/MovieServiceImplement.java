package com.cinehub.movie.service.impl;

import com.cinehub.movie.service.MovieService;
import com.cinehub.movie.dto.AddMovieFromTmdbRequest;
import com.cinehub.movie.dto.BulkAddMoviesRequest;
import com.cinehub.movie.dto.BulkAddMoviesResponse;
import com.cinehub.movie.dto.MovieDetailResponse;
import com.cinehub.movie.dto.MovieSummaryResponse;
import com.cinehub.movie.dto.TMDb.TMDbCreditsResponse;
import com.cinehub.movie.dto.TMDb.TMDbMovieResponse;
import com.cinehub.movie.dto.TMDb.TMDbReleaseDatesResponse;
import com.cinehub.movie.entity.MovieDetail;
import com.cinehub.movie.entity.MovieStatus;
import com.cinehub.movie.entity.MovieSummary;
import com.cinehub.movie.entity.MovieTranslation;
import com.cinehub.movie.mapper.MovieMapper;
import com.cinehub.movie.dto.response.PagedResponse;
import com.cinehub.movie.repository.MovieDetailRepository;
import com.cinehub.movie.repository.MovieSummaryRepository;
import com.cinehub.movie.repository.MovieTranslationRepository;
import com.cinehub.movie.service.client.TMDbClient;
import com.cinehub.movie.util.AgeRatingNormalizer;
import com.cinehub.movie.util.RegexUtil;
import org.springframework.data.domain.Sort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
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
    private final MovieTranslationRepository movieTranslationRepository;
    private final TMDbClient tmdbClient;
    private final MovieMapper movieMapper;
    private final MongoTemplate mongoTemplate;

    public MovieDetailResponse getMovieByUuid(UUID id, String language) {
        MovieDetail entity = movieDetailRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Movie not found with UUID: " + id));

        MovieDetailResponse response = movieMapper.toDetailResponse(entity);
        applyTranslation(response, language);
        return response;
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
            TMDbMovieResponse fullMovieEn = tmdbClient.fetchMovieDetailInEnglish(movie.getId());
            syncMovie(fullMovie, fullMovieEn, MovieStatus.NOW_PLAYING);
        }
        for (TMDbMovieResponse movie : upcoming) {
            TMDbMovieResponse fullMovie = tmdbClient.fetchMovieDetail(movie.getId());
            TMDbMovieResponse fullMovieEn = tmdbClient.fetchMovieDetailInEnglish(movie.getId());
            syncMovie(fullMovie, fullMovieEn, MovieStatus.UPCOMING);
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

    private void syncMovie(TMDbMovieResponse movie, TMDbMovieResponse movieEn, MovieStatus status) {

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
        summary.setPopularity(movie.getPopularity());

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
        detail.setPopularity(movie.getPopularity());

        movieDetailRepository.save(detail);

        // --- Save Translations (Vietnamese) ---
        MovieTranslation translationVi = movieTranslationRepository
                .findByMovieIdAndLanguage(sharedId, "vi")
                .orElse(new MovieTranslation());

        translationVi.setMovieId(sharedId);
        translationVi.setLanguage("vi");
        translationVi.setTitle(movie.getTitle());
        translationVi.setOverview(movie.getOverview());
        translationVi.setGenres(movie.getGenres().stream()
                .map(TMDbMovieResponse.Genre::getName)
                .toList());
        translationVi.setCountry(movie.getProductionCountries().isEmpty() ? null
                : movie.getProductionCountries().get(0).getName());

        movieTranslationRepository.save(translationVi);

        // --- Save Translations (English) ---
        MovieTranslation translationEn = movieTranslationRepository
                .findByMovieIdAndLanguage(sharedId, "en")
                .orElse(new MovieTranslation());

        translationEn.setMovieId(sharedId);
        translationEn.setLanguage("en");
        translationEn.setTitle(movieEn.getTitle());
        translationEn.setOverview(movieEn.getOverview());
        translationEn.setGenres(movieEn.getGenres().stream()
                .map(TMDbMovieResponse.Genre::getName)
                .toList());
        translationEn.setCountry(movieEn.getProductionCountries().isEmpty() ? null
                : movieEn.getProductionCountries().get(0).getName());

        movieTranslationRepository.save(translationEn);

        log.info("Synced movie: {} ({}) with translations (vi, en)", movie.getTitle(), movie.getId());
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

    /**
     * Apply translation to MovieSummaryResponse based on language
     */
    private void applyTranslation(MovieSummaryResponse response, String language) {
        if (language == null || language.equals("vi")) {
            return; // Default is Vietnamese
        }

        movieTranslationRepository.findByMovieIdAndLanguage(response.getId(), language)
                .ifPresent(translation -> {
                    response.setTitle(translation.getTitle());
                    response.setGenres(translation.getGenres());
                });
    }

    /**
     * Apply translation to MovieDetailResponse based on language
     */
    private void applyTranslation(MovieDetailResponse response, String language) {
        if (language == null || language.equals("vi")) {
            return; // Default is Vietnamese
        }

        movieTranslationRepository.findByMovieIdAndLanguage(response.getId(), language)
                .ifPresent(translation -> {
                    response.setTitle(translation.getTitle());
                    response.setOverview(translation.getOverview());
                    response.setGenres(translation.getGenres());
                    response.setCountry(translation.getCountry());
                });
    }

    public Page<MovieSummaryResponse> getNowPlayingMovies(Pageable pageable, String language) {
        Pageable sortedPageable = createPageableWithPopularitySort(pageable);
        Page<MovieSummary> entities = movieSummaryRepository.findByStatus(MovieStatus.NOW_PLAYING, sortedPageable);
        Page<MovieSummaryResponse> responses = movieMapper.toSummaryResponsePage(entities);

        // Apply translations
        responses.getContent().forEach(response -> applyTranslation(response, language));

        return responses;
    }

    public Page<MovieSummaryResponse> getUpcomingMovies(Pageable pageable, String language) {
        Pageable sortedPageable = createPageableWithPopularitySort(pageable);
        Page<MovieSummary> entities = movieSummaryRepository.findByStatus(MovieStatus.UPCOMING, sortedPageable);
        Page<MovieSummaryResponse> responses = movieMapper.toSummaryResponsePage(entities);

        // Apply translations
        responses.getContent().forEach(response -> applyTranslation(response, language));

        return responses;
    }

    public Page<MovieSummaryResponse> getArchivedMovies(Pageable pageable) {
        Pageable sortedPageable = createPageableWithPopularitySort(pageable);
        Page<MovieSummary> entities = movieSummaryRepository.findByStatus(MovieStatus.ARCHIVED, sortedPageable);
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

    public MovieDetailResponse getMovieDetail(Integer tmdbId, String language) {
        if (tmdbId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TMDb ID is required");
        }

        Optional<MovieDetail> movieDetail = movieDetailRepository.findByTmdbId(tmdbId);
        if (movieDetail.isPresent()) {
            MovieDetailResponse response = movieMapper.toDetailResponse(movieDetail.get());
            applyTranslation(response, language);
            return response;
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

        if (request.getPosterUrl() != null) {
            existingDetail.setPosterUrl(request.getPosterUrl());
            existingSummary.setPosterUrl(request.getPosterUrl());
        }

        if (request.getGenres() != null && !request.getGenres().isEmpty()) {
            existingDetail.setGenres(request.getGenres());
            existingSummary.setGenres(request.getGenres());
        }

        if (request.getTime() != null) {
            existingDetail.setTime(request.getTime());
            existingSummary.setTime(request.getTime());
        }

        if (request.getCountry() != null) {
            existingDetail.setCountry(request.getCountry());
            existingSummary.setCountry(request.getCountry());
        }

        if (request.getTrailer() != null) {
            existingDetail.setTrailer(request.getTrailer());
            existingSummary.setTrailer(request.getTrailer());
        }

        if (request.getAge() != null) {
            existingDetail.setAge(request.getAge());
            existingSummary.setAge(request.getAge());
        }

        movieDetailRepository.save(existingDetail);
        movieSummaryRepository.save(existingSummary);

        return movieMapper.toDetailResponse(existingDetail);
    }

    @Override
    public PagedResponse<MovieSummaryResponse> adminSearch(String keyword,
            MovieStatus status,
            String genres,
            int page,
            int size,
            String sortBy,
            String sortType) {

        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "createdAt";
        List<String> allowedSort = List.of("createdAt", "title", "status", "popularity", "startDate", "endDate",
                "time");
        if (!allowedSort.contains(sortField)) {
            sortField = "createdAt";
        }

        String order = (sortType != null && !sortType.isBlank()) ? sortType : "DESC";
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(order);
        } catch (IllegalArgumentException ex) {
            direction = Sort.Direction.DESC;
        }

        int pageIndex = Math.max(1, page) - 1;
        int safeSize = Math.max(1, size);
        Pageable pageable = PageRequest.of(pageIndex, safeSize, Sort.by(direction, sortField));

        // --- build query ---
        Query query = new Query();

        if (keyword != null && !keyword.isBlank()) {
            String escaped = RegexUtil.escape(keyword.trim());
            query.addCriteria(Criteria.where("title").regex(".*" + escaped + ".*", "i"));
        }

        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }

        if (genres != null && !genres.isBlank()) {
            // Spring auto-decodes URL, no need for URLDecoder
            query.addCriteria(Criteria.where("genres").in(genres));
        }

        // --- total count (before paging) ---
        long total = mongoTemplate.count(query, MovieSummary.class);

        // --- apply paging & sort then fetch ---
        query.with(pageable);
        List<MovieSummary> list = mongoTemplate.find(query, MovieSummary.class);

        Page<MovieSummary> entityPage = new PageImpl<>(list, pageable, total);

        // --- convert to DTO page ---
        Page<MovieSummaryResponse> dtoPage = movieMapper.toSummaryResponsePage(entityPage);

        // --- return PagedResponse with 1-based page ---
        return PagedResponse.<MovieSummaryResponse>builder()
                .data(dtoPage.getContent())
                .page(dtoPage.getNumber() + 1)
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

        // Update MovieSummary
        MovieSummary summary = movieSummaryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));
        summary.setStatus(status);
        movieSummaryRepository.save(summary);

        // Update MovieDetail
        MovieDetail detail = movieDetailRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie detail not found"));
        detail.setStatus(status);
        movieDetailRepository.save(detail);

        log.info("Changed movie status for id={} to {}", id, status);
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

    @Override
    @Transactional
    public MovieDetailResponse addMovieFromTmdb(AddMovieFromTmdbRequest request) {
        log.info("Adding movie from TMDB: tmdbId={}, startDate={}, endDate={}",
                request.getTmdbId(), request.getStartDate(), request.getEndDate());

        // Check if movie already exists
        if (movieSummaryRepository.existsByTmdbId(request.getTmdbId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Movie with TMDb ID " + request.getTmdbId() + " already exists");
        }

        // Fetch movie data from TMDB (both Vietnamese and English)
        TMDbMovieResponse movieResponse = tmdbClient.fetchMovieDetail(request.getTmdbId());
        if (movieResponse == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Movie not found in TMDB with ID: " + request.getTmdbId());
        }

        TMDbMovieResponse movieResponseEn = tmdbClient.fetchMovieDetailInEnglish(request.getTmdbId());
        TMDbCreditsResponse credits = tmdbClient.fetchCredits(request.getTmdbId());
        TMDbReleaseDatesResponse releaseDates = tmdbClient.fetchReleaseDates(request.getTmdbId());
        String trailer = tmdbClient.fetchTrailerKey(request.getTmdbId());
        String age = AgeRatingNormalizer.normalize(extractAgeRating(releaseDates));

        // Determine status based on startDate
        LocalDate today = LocalDate.now();
        MovieStatus status;
        if (request.getStartDate().isAfter(today)) {
            status = MovieStatus.UPCOMING;
        } else if (request.getEndDate() == null || !request.getEndDate().isBefore(today)) {
            status = MovieStatus.NOW_PLAYING;
        } else {
            status = MovieStatus.ARCHIVED;
        }

        UUID sharedId = UUID.randomUUID();

        // Create MovieSummary
        MovieSummary summary = new MovieSummary();
        summary.setId(sharedId);
        summary.setTmdbId(movieResponse.getId());
        summary.setTitle(movieResponse.getTitle());
        summary.setPosterUrl(movieResponse.getPosterPath());
        summary.setStatus(status);
        summary.setSpokenLanguages(
                movieResponse.getSpokenLanguages().stream()
                        .map(TMDbMovieResponse.SpokenLanguage::getIso6391)
                        .toList());
        summary.setCountry(
                movieResponse.getProductionCountries().isEmpty() ? null
                        : movieResponse.getProductionCountries().get(0).getName());
        summary.setTime(movieResponse.getRuntime());
        summary.setGenres(movieResponse.getGenres().stream()
                .map(TMDbMovieResponse.Genre::getName).toList());
        summary.setAge(age);
        summary.setTrailer(trailer);
        summary.setStartDate(request.getStartDate());
        summary.setEndDate(request.getEndDate());
        summary.setPopularity(movieResponse.getPopularity());

        movieSummaryRepository.save(summary);

        // Create MovieDetail
        MovieDetail detail = new MovieDetail();
        detail.setId(sharedId);
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
        detail.setGenres(movieResponse.getGenres().stream()
                .map(TMDbMovieResponse.Genre::getName).toList());
        detail.setCast(
                credits.getCast().stream()
                        .map(TMDbCreditsResponse.Cast::getName).limit(10).toList());
        detail.setCrew(
                credits.getCrew().stream()
                        .filter(c -> "Director".equalsIgnoreCase(c.getJob()))
                        .map(TMDbCreditsResponse.Crew::getName)
                        .toList());
        detail.setAge(age);
        detail.setTrailer(trailer);
        detail.setPosterUrl(movieResponse.getPosterPath());
        detail.setStatus(status);
        detail.setStartDate(request.getStartDate());
        detail.setEndDate(request.getEndDate());
        detail.setPopularity(movieResponse.getPopularity());

        movieDetailRepository.save(detail);

        // --- Save Translations (Vietnamese) ---
        MovieTranslation translationVi = new MovieTranslation();
        translationVi.setId(UUID.randomUUID());
        translationVi.setMovieId(sharedId);
        translationVi.setLanguage("vi");
        translationVi.setTitle(movieResponse.getTitle());
        translationVi.setOverview(movieResponse.getOverview());
        translationVi.setGenres(movieResponse.getGenres().stream()
                .map(TMDbMovieResponse.Genre::getName)
                .toList());
        translationVi.setCountry(movieResponse.getProductionCountries().isEmpty() ? null
                : movieResponse.getProductionCountries().get(0).getName());

        movieTranslationRepository.save(translationVi);

        // --- Save Translations (English) ---
        MovieTranslation translationEn = new MovieTranslation();
        translationEn.setId(UUID.randomUUID());
        translationEn.setMovieId(sharedId);
        translationEn.setLanguage("en");
        translationEn.setTitle(movieResponseEn.getTitle());
        translationEn.setOverview(movieResponseEn.getOverview());
        translationEn.setGenres(movieResponseEn.getGenres().stream()
                .map(TMDbMovieResponse.Genre::getName)
                .toList());
        translationEn.setCountry(movieResponseEn.getProductionCountries().isEmpty() ? null
                : movieResponseEn.getProductionCountries().get(0).getName());

        movieTranslationRepository.save(translationEn);

        log.info("Added movie from TMDB with translations: {} ({}), status={}", movieResponse.getTitle(),
                movieResponse.getId(), status);

        return movieMapper.toDetailResponse(detail);
    }

    @Override
    public BulkAddMoviesResponse bulkAddMoviesFromTmdb(BulkAddMoviesRequest request) {
        log.info("Bulk adding {} movies from TMDB", request.getMovies().size());

        int totalRequested = request.getMovies().size();
        int successCount = 0;
        int failedCount = 0;
        List<BulkAddMoviesResponse.MovieAddResult> results = new ArrayList<>();

        for (AddMovieFromTmdbRequest movieRequest : request.getMovies()) {
            try {
                MovieDetailResponse movieDetail = addMovieFromTmdb(movieRequest);

                results.add(BulkAddMoviesResponse.MovieAddResult.builder()
                        .tmdbId(movieRequest.getTmdbId())
                        .success(true)
                        .message("Successfully added")
                        .movieDetail(movieDetail)
                        .build());

                successCount++;
            } catch (Exception e) {
                log.error("Failed to add movie with TMDb ID {}: {}",
                        movieRequest.getTmdbId(), e.getMessage());

                results.add(BulkAddMoviesResponse.MovieAddResult.builder()
                        .tmdbId(movieRequest.getTmdbId())
                        .success(false)
                        .message(e.getMessage())
                        .movieDetail(null)
                        .build());

                failedCount++;
            }
        }

        log.info("Bulk add completed: {} success, {} failed out of {} total",
                successCount, failedCount, totalRequested);

        return BulkAddMoviesResponse.builder()
                .totalRequested(totalRequested)
                .successCount(successCount)
                .failedCount(failedCount)
                .results(results)
                .build();
    }

    @Override
    public List<MovieSummaryResponse> getAvailableMoviesForDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Getting available movies for date range: {} to {}", startDate, endDate);

        List<MovieSummary> activeMovies = movieSummaryRepository.findByStatusIn(
                List.of(MovieStatus.NOW_PLAYING, MovieStatus.UPCOMING));

        return activeMovies.stream()
                .filter(movie -> isMovieAvailableInRange(movie, startDate, endDate))
                .map(movieMapper::toSummaryResponse)
                .toList();
    }

    private boolean isMovieAvailableInRange(MovieSummary movie, LocalDate rangeStart, LocalDate rangeEnd) {
        if (movie.getStartDate() == null) {
            return false;
        }

        if (movie.getStartDate().isAfter(rangeEnd)) {
            return false;
        }

        if (movie.getEndDate() != null && movie.getEndDate().isBefore(rangeStart)) {
            return false;
        }

        return true;
    }

    private Pageable createPageableWithPopularitySort(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "popularity") // Sắp xếp điểm cao nhất lên đầu
        );
    }

    @Override
    public Map<UUID, String> getBatchMovieTitles(List<UUID> movieIds) {
        if (movieIds == null || movieIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<MovieDetail> movies = movieDetailRepository.findAllByIdIn(movieIds);
        return movies.stream()
                .collect(Collectors.toMap(
                        MovieDetail::getId,
                        movie -> movie.getTitle() != null ? movie.getTitle() : "Unknown Movie"));
    }
}
