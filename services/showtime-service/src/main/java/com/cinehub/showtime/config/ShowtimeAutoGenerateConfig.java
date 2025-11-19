package com.cinehub.showtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "showtime.auto-generate")
@Data
public class ShowtimeAutoGenerateConfig {

    /**
     * Operating start hour (0-23)
     * Default: 5 (5:00 AM)
     */
    private int startHour = 5;

    /**
     * Operating end hour (0-24)
     * Default: 24 (midnight)
     */
    private int endHour = 24;

    /**
     * Cleaning gap between showtimes in minutes
     * Default: 20 minutes
     */
    private int cleaningGapMinutes = 20;
}
