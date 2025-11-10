package com.cinehub.auth.repository;

import com.cinehub.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Optional<User> findByNationalId(String nationalId);

    Boolean existsByNationalId(String nationalId);

    @Query("SELECT u FROM User u WHERE u.email = :identifier OR u.username = :identifier OR u.phoneNumber = :identifier")
    Optional<User> findByEmailOrUsernameOrPhoneNumber(@Param("identifier") String identifier);

    long countByRole_NameIgnoreCase(String roleName);
}
