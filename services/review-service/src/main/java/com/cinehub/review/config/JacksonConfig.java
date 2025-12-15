package com.cinehub.review.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@Configuration
public class JacksonConfig {

    @PostConstruct
    public void init() {
        // Set default JVM timezone to Vietnam
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        // Prevent infinite recursion
        mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);

        return mapper;
    }
}
