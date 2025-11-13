package com.cinehub.gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class MovieAndTheaterSearchController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    @Value("${services.movie.base-url:http://movie-service:8083}")
    private String movieServiceBase;

    @Value("${services.theater.base-url:http://showtime-service:8084}")
    private String theaterServiceBase;

    @GetMapping
    public ResponseEntity<JsonNode> search(
            @RequestParam String keyword,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId,
            @RequestHeader(value = "X-User-Role", required = false) String xUserRole) {

        // validate
        if (keyword == null || keyword.isBlank()) {
            var err = objectMapper.createObjectNode()
                    .put("error", "keyword is required");
            return ResponseEntity.badRequest().body(err);
        }

        List<String> errors = new CopyOnWriteArrayList<>();

        // prepare forwarded internal headers
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        if (xUserId != null && !xUserId.isBlank())
            headers.set("X-User-Id", xUserId);

        if (xUserRole != null && !xUserRole.isBlank())
            headers.set("X-User-Role", xUserRole);

        // no need to forward Authorization
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // ------- MOVIE SERVICE -------
        CompletableFuture<JsonNode> moviesFuture = CompletableFuture.supplyAsync(() -> {
            String url = String.format("%s/api/movies/search?keyword=%s",
                    movieServiceBase, urlEncode(keyword));

            try {
                JsonNode resp = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class).getBody();

                if (resp == null || !resp.isArray())
                    return objectMapper.createArrayNode();

                return resp;
            } catch (Exception ex) {
                log.warn("Movie service error: {}", unwrap(ex));
                errors.add("Movie service error: " + unwrap(ex));
                return objectMapper.createArrayNode();
            }
        }, executor);

        // ------- THEATER SERVICE -------
        CompletableFuture<JsonNode> theatersFuture = CompletableFuture.supplyAsync(() -> {
            String url = String.format("%s/api/showtimes/theaters/search?keyword=%s",
                    theaterServiceBase, urlEncode(keyword));

            try {
                JsonNode resp = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class).getBody();

                if (resp == null || !resp.isArray())
                    return objectMapper.createArrayNode();

                return resp;
            } catch (Exception ex) {
                log.warn("Theater service error: {}", unwrap(ex));
                errors.add("Theater service error: " + unwrap(ex));
                return objectMapper.createArrayNode();
            }
        }, executor);

        // wait (with timeout but allow partial)
        try {
            CompletableFuture.allOf(moviesFuture, theatersFuture).get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Aggregator timeout: {}", unwrap(e));
        }

        JsonNode movieNode = moviesFuture.join();
        JsonNode theaterNode = theatersFuture.join();

        var root = objectMapper.createObjectNode();
        root.set("movies", movieNode);
        root.set("theaters", theaterNode);
        root.put("partial", !errors.isEmpty());
        root.set("errors", objectMapper.valueToTree(errors));

        return ResponseEntity.ok(root);
    }

    // Helpers
    private static String urlEncode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private static String unwrap(Throwable ex) {
        Throwable c = (ex instanceof CompletionException && ex.getCause() != null)
                ? ex.getCause()
                : ex;
        return c.getMessage() == null ? c.toString() : c.getMessage();
    }
}
