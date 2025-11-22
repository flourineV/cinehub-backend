package com.cinehub.showtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "showtime.auto-generate")
@Data
public class ShowtimeAutoGenerateConfig {

    private int startHour = 5;
    private int endHour = 24;
    private int cleaningGapMinutes = 20;
}
