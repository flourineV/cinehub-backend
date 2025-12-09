package com.cinehub.notification.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileClient {

    private final WebClient userProfileWebClient;

    public UserProfileResponse getUserProfile(String userId) {
        try {
            return userProfileWebClient.get()
                    .uri("/api/profiles/profiles/{userId}", userId)
                    .retrieve()
                    .bodyToMono(UserProfileResponse.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch user profile for userId {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public List<String> getSubscribedUsersEmails() {
        try {
            return userProfileWebClient.get()
                    .uri("/api/profiles/profiles/subscribed-emails")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<String>>() {
                    })
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch subscribed users emails: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public record UserProfileResponse(
            String email,
            String fullName,
            String username) {
    }
}
