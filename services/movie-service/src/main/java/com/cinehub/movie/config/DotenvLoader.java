package com.cinehub.movie.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DotenvLoader {
    static {
        Dotenv dotenv = Dotenv.load();
        System.setProperty("SERVER_PORT", dotenv.get("SERVER_PORT"));
        System.setProperty("SERVER_CONTEXT_PATH", dotenv.get("SERVER_CONTEXT_PATH"));
        System.setProperty("MONGO_USERNAME", dotenv.get("MONGO_USERNAME"));
        System.setProperty("MONGO_PASSWORD", dotenv.get("MONGO_PASSWORD"));
        System.setProperty("MONGO_CLUSTER", dotenv.get("MONGO_CLUSTER"));
        System.setProperty("MONGO_DB", dotenv.get("MONGO_DB"));
        System.setProperty("MONGO_OPTIONS", dotenv.get("MONGO_OPTIONS"));
    }
}
