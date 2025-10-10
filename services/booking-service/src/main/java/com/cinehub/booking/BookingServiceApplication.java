package com.cinehub.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }

    // 🧩 RestTemplate bean dùng cho các client như PricingClient, ShowtimeClient...
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
