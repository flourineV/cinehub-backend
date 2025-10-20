package com.cinehub.profile.service;

import com.cinehub.profile.entity.StaffProfile;
import com.cinehub.profile.entity.UserProfile;
import com.cinehub.profile.exception.ResourceNotFoundException;
import com.cinehub.profile.repository.StaffProfileRepository;
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
public class StaffProfileService {

    private final StaffProfileRepository staffRepository;
    private final UserProfileRepository userProfileRepository;

    public StaffProfile createStaff(UUID userProfileId, UUID cinemaId, LocalDate startDate) {
        UserProfile profile = userProfileRepository.findById(userProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + userProfileId));

        if (staffRepository.existsByUserProfile_Id(userProfileId)) {
            throw new IllegalArgumentException("This user already has a staff profile.");
        }

        StaffProfile staff = StaffProfile.builder()
                .userProfile(profile)
                .cinemaId(cinemaId)
                .startDate(startDate)
                .build();

        return staffRepository.save(staff);
    }

    @Transactional(readOnly = true)
    public List<StaffProfile> getStaffByCinema(UUID cinemaId) {
        return staffRepository.findByCinemaId(cinemaId);
    }

    @Transactional(readOnly = true)
    public StaffProfile getStaffByUserProfileId(UUID userProfileId) {
        return staffRepository.findByUserProfile_Id(userProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found for user: " + userProfileId));
    }

    @Transactional(readOnly = true)
    public List<StaffProfile> getAllStaff() {
        return staffRepository.findAll();
    }

    public void deleteStaff(UUID staffId) {
        if (!staffRepository.existsById(staffId)) {
            throw new ResourceNotFoundException("Staff not found with id: " + staffId);
        }
        staffRepository.deleteById(staffId);
    }
}
