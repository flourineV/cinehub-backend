package com.cinehub.fnb.service;

import com.cinehub.fnb.dto.request.FnbOrderRequest;
import com.cinehub.fnb.dto.response.FnbOrderItemResponse;
import com.cinehub.fnb.dto.response.FnbOrderResponse;
import com.cinehub.fnb.entity.*;
import com.cinehub.fnb.repository.FnbItemRepository;
import com.cinehub.fnb.repository.FnbOrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class FnbOrderService {

    private final FnbOrderRepository fnbOrderRepository;
    private final FnbItemRepository fnbItemRepository;

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
                    .totalPrice(subtotal)
                    .build();
        }).toList();

        FnbOrder order = FnbOrder.builder()
                .userId(request.getUserId())
                .orderCode("FNB-" + System.currentTimeMillis())
                .status(FnbOrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .totalAmount(total.get())
                .createdAt(LocalDateTime.now())
                .build();

        orderItems.forEach(i -> i.setOrder(order));
        order.setItems(orderItems);

        FnbOrder saved = fnbOrderRepository.save(order);
        return mapToResponse(saved);
    }

    public List<FnbOrderResponse> getOrdersByUser(UUID userId) {
        return fnbOrderRepository.findByUserId(userId).stream()
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
        return FnbOrderResponse.builder()
                .id(o.getId())
                .userId(o.getUserId())
                .orderCode(o.getOrderCode())
                .status(o.getStatus().name())
                .paymentMethod(o.getPaymentMethod())
                .totalAmount(o.getTotalAmount())
                .createdAt(o.getCreatedAt())
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
