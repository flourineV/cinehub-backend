package com.cinehub.profile.repository;

import com.cinehub.profile.entity.LoyaltyHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoyaltyHistoryRepository extends JpaRepository<LoyaltyHistory, UUID> {
    
    Page<LoyaltyHistory> findByUser_UserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    long countByUser_UserId(UUID userId);
}
