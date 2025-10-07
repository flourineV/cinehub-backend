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
        // Trả về HTTP 404 và nội dung thông báo lỗi (ví dụ: "Room with ID... not
        // found")
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
}