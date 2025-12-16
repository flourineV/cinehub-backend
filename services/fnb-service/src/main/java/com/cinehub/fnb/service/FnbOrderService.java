package com.cinehub.fnb.service;

import com.cinehub.fnb.dto.request.FnbOrderRequest;
import com.cinehub.fnb.dto.response.FnbOrderItemResponse;
import com.cinehub.fnb.dto.response.FnbOrderResponse;
import com.cinehub.fnb.entity.*;
import com.cinehub.fnb.events.FnbOrderCreatedEvent;
import com.cinehub.fnb.producer.FnbProducer;
import com.cinehub.fnb.repository.FnbItemRepository;
import com.cinehub.fnb.repository.FnbOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class FnbOrderService {

        private final FnbOrderRepository fnbOrderRepository;
        private final FnbItemRepository fnbItemRepository;
        private final FnbProducer fnbProducer;
        private final StringRedisTemplate redisTemplate;

        @Transactional
        public FnbOrderResponse createOrder(FnbOrderRequest request) {
                AtomicReference<BigDecimal> total = new AtomicReference<>(BigDecimal.ZERO);

                List<FnbOrderItem> orderItems = request.getItems().stream().map(i -> {
                        var item = fnbItemRepository.findById(i.getFnbItemId())
                                        .orElseThrow(() -> new IllegalArgumentException("FNB item not found"));

                        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
                        total.set(total.get().add(subtotal));

                        return FnbOrderItem.builder()
                                        .fnbItemId(item.getId())
                                        .quantity(i.getQuantity())
                                        .unitPrice(item.getUnitPrice())
                                        // totalPrice is auto-calculated by database (generated column)
                                        .build();
                }).toList();

                FnbOrder order = FnbOrder.builder()
                                .userId(request.getUserId())
                                .theaterId(request.getTheaterId())
                                .orderCode("FNB-" + System.currentTimeMillis())
                                .status(FnbOrderStatus.PENDING)
                                .paymentMethod(request.getPaymentMethod())
                                .totalAmount(total.get())
                                .createdAt(LocalDateTime.now())
                                .build();

                orderItems.forEach(i -> i.setOrder(order));
                order.setItems(orderItems);

                FnbOrder saved = fnbOrderRepository.save(order);

                // Create Redis key with 5-minute TTL for auto-cancellation
                String redisKey = "fnb_order:" + saved.getId();
                redisTemplate.opsForValue().set(redisKey, "PENDING", 5, TimeUnit.MINUTES);
                log.info("Created Redis TTL key: {} (expires in 5 minutes)", redisKey);

                // Send event to Payment Service
                fnbProducer.sendFnbOrderCreatedEvent(new FnbOrderCreatedEvent(
                                saved.getId(),
                                saved.getUserId(),
                                saved.getTheaterId(),
                                saved.getTotalAmount()));

                log.info("FnbOrder created: {} | theaterId={} | total={}",
                                saved.getId(), saved.getTheaterId(), saved.getTotalAmount());

                return mapToResponse(saved);
        }

        public List<FnbOrderResponse> getOrdersByUser(UUID userId) {
                return fnbOrderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        public FnbOrderResponse getById(UUID id) {
                return fnbOrderRepository.findById(id)
                                .map(this::mapToResponse)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        }

        @Transactional
        public void cancelOrder(UUID id) {
                var order = fnbOrderRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
                order.setStatus(FnbOrderStatus.CANCELLED);
                fnbOrderRepository.save(order);
        }

        private FnbOrderResponse mapToResponse(FnbOrder o) {
                // Calculate expiration time for PENDING orders (5 minutes TTL)
                LocalDateTime expiresAt = o.getStatus() == FnbOrderStatus.PENDING
                                ? o.getCreatedAt().plusMinutes(5)
                                : null;

                return FnbOrderResponse.builder()
                                .id(o.getId())
                                .userId(o.getUserId())
                                .theaterId(o.getTheaterId())
                                .orderCode(o.getOrderCode())
                                .status(o.getStatus().name())
                                .paymentMethod(o.getPaymentMethod())
                                .totalAmount(o.getTotalAmount())
                                .createdAt(o.getCreatedAt())
                                .expiresAt(expiresAt)
                                .items(o.getItems().stream()
                                                .map(i -> FnbOrderItemResponse.builder()
                                                                .fnbItemId(i.getFnbItemId())
                                                                .quantity(i.getQuantity())
                                                                .unitPrice(i.getUnitPrice())
                                                                .totalPrice(i.getTotalPrice())
                                                                .build())
                                                .toList())
                                .build();
        }
}
