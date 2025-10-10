package com.cinehub.payment.consumer;

import com.cinehub.payment.config.RabbitConfig;
import com.cinehub.payment.events.BookingCreatedEvent;
import com.cinehub.payment.producer.PaymentProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final PaymentProducer paymentProducer;

    @RabbitListener(queues = RabbitConfig.PAYMENT_QUEUE)
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("📥 [PaymentConsumer] Received BookingCreatedEvent | bookingId={} | total={} | seats={}",
                event.bookingId(), event.totalPrice(), event.seatIds().size());

        try {
            // 1️⃣ Giả lập xử lý thanh toán (mock 2s)
            Thread.sleep(2000);
            log.info("💳 Payment simulated successfully for bookingId={}", event.bookingId());

            // 2️⃣ Gửi event PaymentCompleted sang NotificationService
            paymentProducer.sendPaymentSuccessEvent(event);

        } catch (Exception e) {
            log.error("❌ Payment failed for bookingId={}: {}", event.bookingId(), e.getMessage());
        }
    }
}
