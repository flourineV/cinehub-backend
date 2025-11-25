package com.cinehub.booking.service;

import com.cinehub.booking.dto.request.BookingCriteria;
import com.cinehub.booking.dto.response.BookingResponse;
import com.cinehub.booking.dto.response.PagedResponse;

public interface BookingStatisticService {

    PagedResponse<BookingResponse> getBookingsByCriteria(
            BookingCriteria criteria,
            int page,
            int size,
            String sortBy,
            String sortType);

}