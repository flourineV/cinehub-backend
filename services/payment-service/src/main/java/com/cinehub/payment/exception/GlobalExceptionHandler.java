package com.cinehub.payment.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.time.Instant;

/**
 * Xử lý các Exception xảy ra trong toàn bộ Payment Service Controllers.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý PaymentProcessingException (Lỗi nghiệp vụ như Transaction không tìm
     * thấy/không PENDING).
     * Trả về HTTP 400 Bad Request hoặc 404 Not Found tùy ngữ cảnh.
     */
    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<Object> handlePaymentProcessingException(PaymentProcessingException ex) {
        log.warn("API Error | Business Logic: {}", ex.getMessage());

        // Sử dụng HttpStatus.BAD_REQUEST (400) hoặc NOT_FOUND (404)
        HttpStatus status = HttpStatus.BAD_REQUEST;

        // Trả về JSON chứa thông tin lỗi
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", ex.getMessage(),
                "path", "" // Trong ControllerAdvice, việc lấy path hiện tại phức tạp hơn
        );

        return new ResponseEntity<>(body, status);
    }

    /**
     * Xử lý lỗi Runtime Exception chung (Nếu có lỗi không được bắt).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneralException(Exception ex) {
        log.error("API Error | Internal Server Error: {}", ex.getMessage(), ex);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", "An unexpected error occurred: " + ex.getMessage(),
                "path", "");

        return new ResponseEntity<>(body, status);
    }
}