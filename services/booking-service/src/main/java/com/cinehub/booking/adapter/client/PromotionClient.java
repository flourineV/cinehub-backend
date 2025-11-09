package com.cinehub.booking.adapter.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.cinehub.booking.dto.external.PromotionValidationResponse;
import com.cinehub.booking.dto.response.RefundVoucherResponse;
import com.cinehub.booking.dto.request.RefundVoucherRequest;
import com.cinehub.booking.entity.DiscountType;
import com.cinehub.booking.exception.BookingException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromotionClient {

    @Qualifier("promotionWebClient")
    private final WebClient promotionWebClient;

    @CircuitBreaker(name = "promotionService", fallbackMethod = "fallbackPromotion")
    public PromotionValidationResponse validatePromotionCode(String promoCode) {

        return promotionWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/promotions/validate")
                        .queryParam("code", promoCode)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {

                    return Mono.error(new BookingException(
                            "Mã khuyến mãi không hợp lệ hoặc đã hết hạn."));
                })
                .bodyToMono(PromotionValidationResponse.class)
                .block();
    }

    @CircuitBreaker(name = "promotionService", fallbackMethod = "fallbackCreateRefundVoucher")
    public RefundVoucherResponse createRefundVoucher(UUID userId, BigDecimal value) {
        RefundVoucherRequest request = RefundVoucherRequest.builder()
                .userId(userId)
                .value(value)
                .expiredAt(LocalDateTime.now().plusMonths(2)) // voucher hết hạn sau 3 tháng
                .build();

        return promotionWebClient.post()
                .uri("/api/promotions/refund-vouchers")
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> Mono.error(new BookingException("Không thể tạo refund voucher.")))
                .bodyToMono(RefundVoucherResponse.class)
                .block();
    }

    @CircuitBreaker(name = "promotionService", fallbackMethod = "fallbackMarkRefundVoucherAsUsed")
    public RefundVoucherResponse markRefundVoucherAsUsed(String code) {
        return promotionWebClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/promotions/refund-vouchers/use/{code}")
                        .build(code))
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> Mono.error(new BookingException("Không thể đánh dấu voucher đã dùng: " + code)))
                .bodyToMono(RefundVoucherResponse.class)
                .block();
    }

    public PromotionValidationResponse fallbackPromotion(String promoCode, Throwable t) {

        System.err.println("Circuit Breaker activated for promotionService. Lỗi: " + t.getMessage());

        return PromotionValidationResponse.builder()
                .code(promoCode)
                .discountType(DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.ZERO)
                .isOneTimeUse(Boolean.FALSE)
                .build();
    }

    public RefundVoucherResponse fallbackCreateRefundVoucher(UUID userId, BigDecimal value, Throwable t) {
        System.err.println(
                "Circuit Breaker activated for promotionService during createRefundVoucher. Lỗi: " + t.getMessage());

        return RefundVoucherResponse.builder()
                .id(UUID.randomUUID())
                .code("N/A")
                .userId(userId)
                .value(BigDecimal.ZERO)
                .isUsed(Boolean.FALSE)
                .build();
    }

    public RefundVoucherResponse fallbackMarkRefundVoucherAsUsed(String code, Throwable t) {
        return RefundVoucherResponse.builder()
                .code(code)
                .isUsed(true)
                .value(BigDecimal.ZERO)
                .build();
    }
}