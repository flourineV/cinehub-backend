package com.cinehub.fnb.listener;

import com.cinehub.fnb.entity.FnbOrder;
import com.cinehub.fnb.entity.FnbOrderStatus;
import com.cinehub.fnb.repository.FnbOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisKeyExpirationListener {

    private final FnbOrderRepository fnbOrderRepository;

    /**
     * Handle Redis key expiration for FnB orders
     * Key format: "fnb_order:{orderId}"
     */
    @Transactional
    public void handleExpiredKey(String expiredKey) {
        if (expiredKey.startsWith("fnb_order:")) {
            log.warn("🚨 FnB order lock expired: {}", expiredKey);

            try {
                // Extract orderId from key: "fnb_order:{orderId}"
                String orderIdStr = expiredKey.substring("fnb_order:".length());
                UUID orderId = UUID.fromString(orderIdStr);

                // Find and cancel the order
                fnbOrderRepository.findById(orderId).ifPresent(order -> {
                    if (order.getStatus() == FnbOrderStatus.PENDING) {
                        order.setStatus(FnbOrderStatus.CANCELLED);
                        fnbOrderRepository.save(order);
                        log.info("❌ Auto-cancelled expired FnB order: {} (TTL expired)",
                                order.getOrderCode());
                    }
                });

            } catch (Exception e) {
                log.error("Error processing expired FnB order key {}: {}",
                        expiredKey, e.getMessage(), e);
            }
        }
    }
}
