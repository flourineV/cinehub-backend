package com.cinehub.profile.repository;

import com.cinehub.profile.entity.ManagerProfile;
import com.cinehub.profile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManagerProfileRepository extends JpaRepository<ManagerProfile, UUID> {

    Optional<ManagerProfile> findByUserProfile(UserProfile userProfile);

    Optional<ManagerProfile> findByUserProfile_Id(UUID userProfileId);

    List<ManagerProfile> findByManagedCinemaId(UUID cinemaId);

    boolean existsByUserProfile_Id(UUID userProfileId);
}
