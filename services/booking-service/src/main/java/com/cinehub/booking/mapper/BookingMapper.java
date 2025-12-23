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
                .seatNumber(seat.getSeatNumber())
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
                .bookingCode(booking.getBookingCode())
                .userId(booking.getUserId())
                .showtimeId(booking.getShowtimeId())
                .movieId(booking.getMovieId())
                .movieTitle(booking.getMovieTitle())
                .movieTitleEn(booking.getMovieTitleEn())
                .theaterName(booking.getTheaterName())
                .theaterNameEn(booking.getTheaterNameEn())
                .roomName(booking.getRoomName())
                .roomNameEn(booking.getRoomNameEn())
                .showDateTime(booking.getShowDateTime() != null ? booking.getShowDateTime().toString() : null)
                .status(booking.getStatus().name())
                .totalPrice(booking.getTotalPrice())
                .discountAmount(booking.getDiscountAmount())
                .finalPrice(booking.getFinalPrice())
                .guestName(booking.getGuestName())
                .guestEmail(booking.getGuestEmail())
                .seats(toSeatResponses(booking.getSeats()))
                .build();
    }
}
