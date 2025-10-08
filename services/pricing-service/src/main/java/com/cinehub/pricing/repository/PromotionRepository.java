package com.cinehub.pricing.repository;

import com.cinehub.pricing.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    // Lấy các khuyến mãi còn hiệu lực tại thời điểm hiện tại
    @Query("SELECT p FROM Promotion p WHERE p.startDate <= :today AND p.endDate >= :today")
    List<Promotion> findActivePromotions(LocalDate today);
 }
