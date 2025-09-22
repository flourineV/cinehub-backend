package com.cinehub.movie.dto.TMDb;

import lombok.Data;

import java.util.List;

@Data
public class TMDbVideoResponse {
    private Integer id;
    private List<Video> results;

    @Data
    public static class Video {
        private String name;   
        private String key;    
        private String site;  
        private String type;   
        private Boolean official;
        private String published_at;
    }
}
