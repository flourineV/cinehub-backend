package com.cinehub.pricing.repository;

import com.cinehub.pricing.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> { }
