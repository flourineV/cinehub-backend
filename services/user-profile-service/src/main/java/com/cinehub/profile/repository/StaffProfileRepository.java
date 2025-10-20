package com.cinehub.profile.repository;

import com.cinehub.profile.entity.StaffProfile;
import com.cinehub.profile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface StaffProfileRepository extends JpaRepository<StaffProfile, UUID> {

    Optional<StaffProfile> findByUserProfile(UserProfile userProfile);

    Optional<StaffProfile> findByUserProfile_Id(UUID userProfileId);

    boolean existsByUserProfile_Id(UUID userProfileId);

    List<StaffProfile> findByCinemaId(UUID cinemaId);

}
