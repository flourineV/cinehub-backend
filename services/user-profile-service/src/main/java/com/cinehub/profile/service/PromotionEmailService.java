package com.cinehub.profile.service;

import com.cinehub.profile.entity.UserProfile;
import com.cinehub.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PromotionEmailService {

    private final UserProfileRepository profileRepository;

    public List<String> getSubscribedUsersEmails() {
        return profileRepository.findByReceivePromoEmailTrue()
                .stream()
                .map(UserProfile::getEmail)
                .toList();
    }
}
