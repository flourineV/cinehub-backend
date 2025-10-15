package com.cinehub.showtime.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFoundException(EntityNotFoundException ex) {
        // HTTP 404: Dùng cho các trường hợp không tìm thấy tài nguyên (ví dụ: Showtime
        // ID không tồn tại)
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // ✅ Bổ sung handler cho IllegalSeatLockException
    @ExceptionHandler(IllegalSeatLockException.class)
    public ResponseEntity<String> handleIllegalSeatLockException(IllegalSeatLockException ex) {
        // HTTP 409: Dùng cho các trường hợp xung đột (Conflict) như tranh chấp tài
        // nguyên (ghế đã bị khóa)
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.CONFLICT);
    }
}