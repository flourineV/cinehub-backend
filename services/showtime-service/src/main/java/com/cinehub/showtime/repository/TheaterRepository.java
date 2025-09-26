package com.cinehub.showtime.repository;

import com.cinehub.showtime.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, String> {
    List<Theater> findByProvinceId(String provinceId);
}