package com.cinehub.booking.controller;

import com.cinehub.booking.dto.request.FinalizeBookingRequest;
import com.cinehub.booking.dto.response.*;
import com.cinehub.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // 1. Lấy Booking theo ID
    @GetMapping("/{id}")
    public BookingResponse getBookingById(@PathVariable UUID id) {
        return bookingService.getBookingById(id);
    }

    // 2. Lấy danh sách Booking của người dùng
    @GetMapping("/user/{userId}")
    public List<BookingResponse> getBookingsByUser(@PathVariable UUID userId) {
        return bookingService.getBookingsByUser(userId);
    }

    @PatchMapping("/{id}/finalize")
    public BookingResponse finalizeBooking(
            @PathVariable("id") UUID bookingId, // Lấy bookingId từ URL
            @RequestBody FinalizeBookingRequest request) {

        // Gọi hàm service với bookingId và request
        return bookingService.finalizeBooking(bookingId, request);
    }

    // 4. Xóa Booking (Chỉ nên dùng cho Testing/Admin)
    @DeleteMapping("/{id}")
    public void deleteBooking(@PathVariable UUID id) {
        bookingService.deleteBooking(id);
    }
}