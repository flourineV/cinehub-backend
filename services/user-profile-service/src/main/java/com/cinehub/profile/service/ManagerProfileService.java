package com.cinehub.profile.service;

import com.cinehub.profile.dto.response.ManagerProfileResponse;
import com.cinehub.profile.entity.ManagerProfile;
import com.cinehub.profile.entity.UserProfile;
import com.cinehub.profile.exception.ResourceNotFoundException;
import com.cinehub.profile.repository.ManagerProfileRepository;
import com.cinehub.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ManagerProfileService {

    private final ManagerProfileRepository managerRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;

    public ManagerProfileResponse createManager(UUID userProfileId, String managedCinemaName, LocalDate hireDate) {
        UserProfile profile = userProfileRepository.findById(userProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + userProfileId));

        if (managerRepository.existsByUserProfile_Id(userProfileId)) {
            throw new IllegalArgumentException("This user already has a manager profile.");
        }

        ManagerProfile manager = ManagerProfile.builder()
                .userProfile(profile)
                .managedCinemaName(managedCinemaName)
                .hireDate(hireDate)
                .build();

        ManagerProfile saved = managerRepository.save(manager);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ManagerProfileResponse getManagerByUserProfileId(UUID userProfileId) {
        ManagerProfile manager = managerRepository.findByUserProfile_Id(userProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found for user: " + userProfileId));
        return toResponse(manager);
    }

    @Transactional(readOnly = true)
    public List<ManagerProfileResponse> getAllManagers() {
        return managerRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ManagerProfileResponse> getManagersByCinema(String cinemaName) {
        return managerRepository.findByManagedCinemaName(cinemaName).stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteManager(UUID managerId) {
        if (!managerRepository.existsById(managerId)) {
            throw new ResourceNotFoundException("Manager not found with id: " + managerId);
        }
        managerRepository.deleteById(managerId);
    }

    private ManagerProfileResponse toResponse(ManagerProfile manager) {
        return ManagerProfileResponse.builder()
                .id(manager.getId())
                .userProfileId(manager.getUserProfile().getId())
                .userProfile(userProfileService.mapToResponse(manager.getUserProfile()))
                .managedCinemaName(manager.getManagedCinemaName())
                .hireDate(manager.getHireDate())
                .createdAt(manager.getCreatedAt())
                .updatedAt(manager.getUpdatedAt())
                .build();
    }
}
