package com.cinehub.booking.service;

import java.util.List;
import java.util.UUID;

import com.cinehub.booking.dto.request.FinalizeBookingRequest;
import com.cinehub.booking.dto.response.BookingResponse;
import com.cinehub.booking.entity.BookingStatus;
import com.cinehub.booking.events.payment.PaymentFailedEvent;
import com.cinehub.booking.events.payment.PaymentSuccessEvent;
import com.cinehub.booking.events.showtime.SeatUnlockedEvent;

public interface BookingService {

    BookingResponse createBooking(com.cinehub.booking.dto.request.CreateBookingRequest request);

    void handleSeatUnlocked(SeatUnlockedEvent data);

    void handlePaymentSuccess(PaymentSuccessEvent data);

    void handlePaymentFailed(PaymentFailedEvent data);

    BookingResponse finalizeBooking(UUID bookingId, FinalizeBookingRequest request);

    void updateBookingStatus(UUID bookingId, BookingStatus newStatus);

    BookingResponse getBookingById(UUID id);

    List<BookingResponse> getBookingsByUser(UUID userId);

    void deleteBooking(UUID id);
}
