package com.cinehub.booking.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // Body
    private Map<String, Object> createBody(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return body;
    }

    // 1. Xử lý lỗi trùng lặp (409)
    @ExceptionHandler(SeatAlreadyLockedException.class)
    public ResponseEntity<Map<String, Object>> handleSeatAlreadyLocked(SeatAlreadyLockedException ex) {
        log.warn("Conflict exception:  {}", ex.getMessage());
        Map<String, Object> body = createBody(HttpStatus.CONFLICT, "Resource Conflict", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // 2. Xử lý lỗi không tìm thấy booking (404 not found)
    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleBookingNotFound(BookingNotFoundException ex) {
        log.warn("Booking not found: {}", ex.getMessage());
        Map<String, Object> body = createBody(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // 3. Xử lý lỗi nghiệp vụ chung (400 Bad Request)
    @ExceptionHandler(BookingException.class)
    public ResponseEntity<Map<String, Object>> handleBookingException(BookingException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        Map<String, Object> body = createBody(HttpStatus.BAD_REQUEST, "Invalid Request", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 4. Xử lý các lỗi khác không được định nghĩa (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        log.error("Unhandled exception: ", ex);
        Map<String, Object> body = createBody(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
