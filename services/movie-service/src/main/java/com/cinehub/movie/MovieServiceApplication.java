package com.cinehub.movie;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class MovieServiceApplication {

    public static void main(String[] args) {
        // Load .env file
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        // Set system properties from .env
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        SpringApplication.run(MovieServiceApplication.class, args);
    }

}
