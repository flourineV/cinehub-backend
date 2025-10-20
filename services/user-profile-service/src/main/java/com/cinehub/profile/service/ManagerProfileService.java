package com.cinehub.profile.service;

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

    public ManagerProfile createManager(UUID userProfileId, UUID managedCinemaId, LocalDate hireDate) {
        UserProfile profile = userProfileRepository.findById(userProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + userProfileId));

        if (managerRepository.existsByUserProfile_Id(userProfileId)) {
            throw new IllegalArgumentException("This user already has a manager profile.");
        }

        ManagerProfile manager = ManagerProfile.builder()
                .userProfile(profile)
                .managedCinemaId(managedCinemaId)
                .hireDate(hireDate)
                .build();

        return managerRepository.save(manager);
    }

    @Transactional(readOnly = true)
    public ManagerProfile getManagerByUserProfileId(UUID userProfileId) {
        return managerRepository.findByUserProfile_Id(userProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found for user: " + userProfileId));
    }

    @Transactional(readOnly = true)
    public List<ManagerProfile> getAllManagers() {
        return managerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ManagerProfile> getManagersByCinema(UUID cinemaId) {
        return managerRepository.findByManagedCinemaId(cinemaId);
    }

    public void deleteManager(UUID managerId) {
        if (!managerRepository.existsById(managerId)) {
            throw new ResourceNotFoundException("Manager not found with id: " + managerId);
        }
        managerRepository.deleteById(managerId);
    }
}
