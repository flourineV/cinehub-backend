package com.cinehub.fnb.repository;

import com.cinehub.fnb.entity.FnbOrder;
import com.cinehub.fnb.entity.FnbOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FnbOrderRepository extends JpaRepository<FnbOrder, UUID> {
    List<FnbOrder> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<FnbOrder> findByUserIdAndStatus(UUID userId, FnbOrderStatus status);

    List<FnbOrder> findByStatusAndCreatedAtBefore(FnbOrderStatus status, LocalDateTime createdAt);
}
