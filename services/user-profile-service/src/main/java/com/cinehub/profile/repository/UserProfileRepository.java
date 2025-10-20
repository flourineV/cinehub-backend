package com.cinehub.profile.repository;

import com.cinehub.profile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByUserId(UUID userId);

    Optional<UserProfile> findByEmail(String email);

    Optional<UserProfile> findByUsername(String username);

    Optional<UserProfile> findByPhoneNumber(String phoneNumber);

    Optional<UserProfile> findByNationalId(String nationalId);

    Boolean existsByEmail(String email);

    Boolean existsByUsername(String username);

    Boolean existsByPhoneNumber(String phoneNumber);

    Boolean existsByNationalId(String nationalId);

    Boolean existsByUserId(UUID userId);

    @Query("SELECT p FROM UserProfile p WHERE p.email = :identifier OR p.username = :identifier OR p.phoneNumber = :identifier")
    Optional<UserProfile> findByEmailOrUsernameOrPhoneNumber(@Param("identifier") String identifier);

    List<UserProfile> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
            String username, String email, String fullName);

    List<UserProfile> findTop20ByOrderByCreatedAtDesc();
}