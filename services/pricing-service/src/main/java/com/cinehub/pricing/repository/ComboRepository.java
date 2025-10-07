package com.cinehub.pricing.repository;

import com.cinehub.pricing.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ComboRepository extends JpaRepository<Combo, UUID> { }
