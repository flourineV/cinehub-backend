package com.cinehub.profile.service;

import com.cinehub.profile.dto.response.StaffProfileResponse;
import com.cinehub.profile.entity.ManagerProfile;
import com.cinehub.profile.entity.StaffProfile;
import com.cinehub.profile.entity.UserProfile;
import com.cinehub.profile.exception.ResourceNotFoundException;
import com.cinehub.profile.repository.ManagerProfileRepository;
import com.cinehub.profile.repository.StaffProfileRepository;
import com.cinehub.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StaffProfileService {

    private final StaffProfileRepository staffRepository;
    private final UserProfileRepository userProfileRepository;
    private final ManagerProfileRepository managerRepository;
    private final UserProfileService userProfileService;

    public StaffProfileResponse createStaff(UUID userProfileId, String cinemaName, LocalDate hireDate) {
        UserProfile profile = userProfileRepository.findById(userProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found: " + userProfileId));

        if (staffRepository.existsByUserProfile_Id(userProfileId)) {
            throw new IllegalArgumentException("This user already has a staff profile.");
        }

        StaffProfile staff = StaffProfile.builder()
                .userProfile(profile)
                .cinemaName(cinemaName)
                .hireDate(hireDate)
                .build();

        StaffProfile saved = staffRepository.save(staff);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StaffProfileResponse> getStaffByCinema(String cinemaName, UUID currentUserId, String userRole) {
        // Admin can view all
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            return staffRepository.findByCinemaName(cinemaName).stream()
                    .map(this::toResponse)
                    .toList();
        }

        // Manager can only view staff from their managed cinema
        if ("MANAGER".equalsIgnoreCase(userRole)) {
            ManagerProfile manager = managerRepository.findByUserProfile_Id(currentUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager profile not found"));

            if (!manager.getManagedCinemaName().equals(cinemaName)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You can only view staff from your managed cinema");
            }

            return staffRepository.findByCinemaName(cinemaName).stream()
                    .map(this::toResponse)
                    .toList();
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }

    @Transactional(readOnly = true)
    public StaffProfileResponse getStaffByUserProfileId(UUID userProfileId) {
        StaffProfile staff = staffRepository.findByUserProfile_Id(userProfileId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found for user: " + userProfileId));
        return toResponse(staff);
    }

    @Transactional(readOnly = true)
    public List<StaffProfileResponse> getAllStaff(UUID currentUserId, String userRole) {
        // Admin can view all staff from all cinemas
        if ("ADMIN".equalsIgnoreCase(userRole)) {
            return staffRepository.findAll().stream()
                    .map(this::toResponse)
                    .toList();
        }

        // Manager can only view staff from their managed cinema
        if ("MANAGER".equalsIgnoreCase(userRole)) {
            ManagerProfile manager = managerRepository.findByUserProfile_Id(currentUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager profile not found"));

            return staffRepository.findByCinemaName(manager.getManagedCinemaName()).stream()
                    .map(this::toResponse)
                    .toList();
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }

    private StaffProfileResponse toResponse(StaffProfile staff) {
        return StaffProfileResponse.builder()
                .id(staff.getId())
                .userProfileId(staff.getUserProfile().getId())
                .userProfile(userProfileService.mapToResponse(staff.getUserProfile()))
                .cinemaName(staff.getCinemaName())
                .hireDate(staff.getHireDate())
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }

    public void deleteStaff(UUID staffId) {
        if (!staffRepository.existsById(staffId)) {
            throw new ResourceNotFoundException("Staff not found with id: " + staffId);
        }
        staffRepository.deleteById(staffId);
    }
}
