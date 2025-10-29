package com.cinehub.fnb.repository;

import com.cinehub.fnb.entity.FnbOrder;
import com.cinehub.fnb.entity.FnbOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FnbOrderRepository extends JpaRepository<FnbOrder, UUID> {
    List<FnbOrder> findByUserId(UUID userId);

    List<FnbOrder> findByUserIdAndStatus(UUID userId, FnbOrderStatus status);
}
