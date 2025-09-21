package com.cinehub.auth.client;

import com.cinehub.auth.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class UserProfileClient {
    
    @Value("${app.user-profile-service.url:http://localhost:8082/profile}")
    private String profileServiceUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public boolean createUserProfile(User user) {
        try {
            String url = profileServiceUrl + "/api/profiles";
            
            CreateProfileRequest request = CreateProfileRequest.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .phoneNumber(user.getPhoneNumber())
                    .nationalId(user.getNationalId())
                    .build();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<CreateProfileRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully created profile for user: {}", user.getId());
                return true;
            } else {
                log.error("Failed to create profile for user: {}, status: {}", user.getId(), response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            log.error("Error creating profile for user: {}, error: {}", user.getId(), e.getMessage(), e);
            return false;
        }
    }
}