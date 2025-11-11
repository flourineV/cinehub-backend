package com.cinehub.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI cinehubAuthAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CineHub Auth Service API")
                        .description("Authentication and user management APIs for CineHub platform.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CineHub Developer Team")
                                .email("dev@cinehub.com")
                                .url("https://cinehub.dev"))
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local Auth Service"),
                        new Server().url("http://localhost:8099").description("Via API Gateway")));
    }
}
