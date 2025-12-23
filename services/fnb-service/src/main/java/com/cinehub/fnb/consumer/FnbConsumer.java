package com.cinehub.fnb.consumer;

import com.cinehub.fnb.config.RabbitConfig;
import com.cinehub.fnb.entity.FnbOrder;
import com.cinehub.fnb.entity.FnbOrderStatus;
import com.cinehub.fnb.events.FnbOrderConfirmedEvent;
import com.cinehub.fnb.producer.FnbProducer;
import com.cinehub.fnb.repository.FnbItemRepository;
import com.cinehub.fnb.repository.FnbOrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FnbConsumer {

    private final ObjectMapper objectMapper;
    private final FnbOrderRepository fnbOrderRepository;
    private final FnbItemRepository fnbItemRepository;
    private final FnbProducer fnbProducer;

    @RabbitListener(queues = RabbitConfig.FNB_QUEUE)
    @Transactional
    public void handlePaymentEvents(
            @Payload Map<String, Object> raw,
            @Header("amqp_receivedRoutingKey") String routingKey) {

        log.info("[FnbConsumer] Received event | RoutingKey: {}", routingKey);

        try {
            Object dataObj = raw.get("data");

            if (RabbitConfig.PAYMENT_FNB_SUCCESS_KEY.equals(routingKey)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) dataObj;

                UUID fnbOrderId = UUID.fromString(data.get("fnbOrderId").toString());
                UUID paymentId = UUID.fromString(data.get("paymentId").toString());

                log.info("[FnbConsumer] Processing PaymentSuccess | fnbOrderId={}", fnbOrderId);

                fnbOrderRepository.findById(fnbOrderId).ifPresentOrElse(
                        order -> {
                            order.setStatus(FnbOrderStatus.PAID);
                            order.setPaymentId(paymentId);
                            order.setPaymentMethod(data.get("method").toString());
                            fnbOrderRepository.save(order);
                            log.info("✅ FnbOrder {} confirmed after payment", fnbOrderId);

                            // Send confirmation email
                            sendOrderConfirmationEmail(order);
                        },
                        () -> log.warn("⚠️ FnbOrder {} not found", fnbOrderId));
            } else if (RabbitConfig.PAYMENT_FNB_FAILED_KEY.equals(routingKey)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) dataObj;

                UUID fnbOrderId = UUID.fromString(data.get("fnbOrderId").toString());
                String reason = data.get("reason").toString();

                log.info("[FnbConsumer] Processing PaymentFailed | fnbOrderId={} | reason={}", fnbOrderId, reason);

                fnbOrderRepository.findById(fnbOrderId).ifPresentOrElse(
                        order -> {
                            order.setStatus(FnbOrderStatus.CANCELLED);
                            fnbOrderRepository.save(order);
                            log.error("❌ FnbOrder {} cancelled due to payment failure: {}", fnbOrderId, reason);
                        },
                        () -> log.warn("⚠️ FnbOrder {} not found", fnbOrderId));
            } else {
                log.warn("Unknown routing key: {}", routingKey);
            }

        } catch (Exception e) {
            log.error("Error processing payment event for RK {}: {}", routingKey, e.getMessage(), e);
        }
    }

    private void sendOrderConfirmationEmail(FnbOrder order) {
        try {
            // Build FnB items details
            List<FnbOrderConfirmedEvent.FnbItemDetail> itemDetails = new ArrayList<>();

            order.getItems().forEach(orderItem -> {
                fnbItemRepository.findById(orderItem.getFnbItemId()).ifPresent(fnbItem -> {
                    itemDetails.add(new FnbOrderConfirmedEvent.FnbItemDetail(
                            fnbItem.getName(),
                            fnbItem.getNameEn(),
                            orderItem.getQuantity(),
                            orderItem.getUnitPrice(),
                            orderItem.getTotalPrice()));
                });
            });

            // Send event to notification service
            FnbOrderConfirmedEvent event = new FnbOrderConfirmedEvent(
                    order.getId(),
                    order.getUserId(),
                    order.getOrderCode(),
                    order.getTheaterId(),
                    order.getTotalAmount(),
                    itemDetails,
                    order.getLanguage() != null ? order.getLanguage() : "vi");

            fnbProducer.sendFnbOrderConfirmedEvent(event);

            log.info("📧 FnB order confirmation event sent for orderCode: {}", order.getOrderCode());
        } catch (Exception e) {
            log.error("Failed to send FnB order confirmation event: {}", e.getMessage(), e);
        }
    }
}
