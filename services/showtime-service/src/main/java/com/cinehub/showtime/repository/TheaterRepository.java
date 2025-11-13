package com.cinehub.showtime.repository;

import com.cinehub.showtime.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, UUID> {
    List<Theater> findByProvinceId(UUID provinceId);

    List<Theater> findByNameContainingIgnoreCase(String name);

}