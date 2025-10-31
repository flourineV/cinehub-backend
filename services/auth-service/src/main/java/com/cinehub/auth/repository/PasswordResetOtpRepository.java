package com.cinehub.auth.repository;

import com.cinehub.auth.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, UUID> {

    Optional<PasswordResetOtp> findByEmailAndOtp(String email, String otp);

    @Query("SELECT o FROM PasswordResetOtp o " +
            "WHERE o.email = :email AND o.expiresAt > :now " +
            "ORDER BY o.createdAt DESC")
    Optional<PasswordResetOtp> findLatestValidOtp(@Param("email") String email, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM PasswordResetOtp o WHERE o.expiresAt < :now")
    void deleteExpiredOtps(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM PasswordResetOtp o WHERE o.email = :email")
    void deleteAllByEmail(@Param("email") String email);
}
