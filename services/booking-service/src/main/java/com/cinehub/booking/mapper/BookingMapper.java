package com.cinehub.booking.mapper;

import com.cinehub.booking.dto.response.BookingResponse;
import com.cinehub.booking.dto.response.BookingSeatResponse;
import com.cinehub.booking.entity.Booking;
import com.cinehub.booking.entity.BookingSeat;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingMapper {

    public BookingSeatResponse toSeatResponse(BookingSeat seat) {
        return BookingSeatResponse.builder()
                .seatId(seat.getSeatId())
                .seatType(seat.getSeatType())
                .ticketType(seat.getTicketType())
                .price(seat.getPrice())
                .build();
    }

    public List<BookingSeatResponse> toSeatResponses(List<BookingSeat> seats) {
        return seats.stream().map(this::toSeatResponse).toList();
    }

    public BookingResponse toBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .showtimeId(booking.getShowtimeId())
                .status(booking.getStatus().name())
                .totalPrice(booking.getTotalPrice())
                .discountAmount(booking.getDiscountAmount())
                .finalPrice(booking.getFinalPrice())
                .guestName(booking.getGuestName())
                .guestEmail(booking.getGuestEmail())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .seats(toSeatResponses(booking.getSeats()))
                .build();
    }
}
