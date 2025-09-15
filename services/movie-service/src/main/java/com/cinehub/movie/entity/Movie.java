package com.cinehub.movie.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "movies") 
public class Movie {
    
    @Id
    private String id;
    
    private String title;
    private String description;
    private Integer duration; // in minutes
    private LocalDate releaseDate;
    private String status; //NOW_SHOWING, COMING_SOON
    private String language;
    private String director;
    private String cast;
    private String posterUrl;
    private String trailerUrl;

    private List<Genre> genres;
    private List<Review> reviews;
    
    private Rating rating;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Constructors
    public Movie() {
    }

    public Movie(String id, String title, String description, Integer duration, LocalDate releaseDate,
                 String status, String language, List<Genre> genres, List<Review> reviews, Rating rating,
                 String director, String cast, String posterUrl, String trailerUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.duration = duration;
        this.releaseDate = releaseDate;
        this.status = status;
        this.language = language;
        this.genres = genres;
        this.reviews = reviews;
        this.rating = rating;
        this.director = director;
        this.cast = cast;
        this.posterUrl = posterUrl;
        this.trailerUrl = trailerUrl;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public String getCast() {
        return cast;
    }

    public void setCast(String cast) {
        this.cast = cast;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    // ===== Inner Classes =====
    public static class Genre {
        private String name;

        public Genre() {
        }

        public Genre(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class Review {
        private String userId;
        private int rating;
        private String comment;
        private LocalDateTime createdAt;

        public Review() {
        }

        public Review(String userId, int rating, String comment, LocalDateTime createdAt) {
            this.userId = userId;
            this.rating = rating;
            this.comment = comment;
            this.createdAt = createdAt;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public int getRating() {
            return rating;
        }

        public void setRating(int rating) {
            this.rating = rating;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }

    public static class Rating {
        private double avg;
        private int count;

        public Rating() {
        }

        public Rating(double avg, int count) {
            this.avg = avg;
            this.count = count;
        }

        public double getAvg() {
            return avg;
        }

        public void setAvg(double avg) {
            this.avg = avg;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
