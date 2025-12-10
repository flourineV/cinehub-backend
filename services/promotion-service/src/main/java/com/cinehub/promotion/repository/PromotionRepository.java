package com.cinehub.promotion.repository;

import com.cinehub.promotion.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    @Query("SELECT p FROM Promotion p WHERE p.code = :code " +
            "AND p.startDate <= :now AND p.endDate >= :now AND p.isActive = TRUE")
    Optional<Promotion> findValidPromotionByCode(String code, LocalDateTime now);

    Optional<Promotion> findByCode(String code);
}