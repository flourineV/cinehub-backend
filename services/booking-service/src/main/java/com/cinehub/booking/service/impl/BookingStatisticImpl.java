package com.cinehub.booking.service.impl;

import com.cinehub.booking.dto.request.BookingCriteria;
import com.cinehub.booking.dto.response.BookingResponse;
import com.cinehub.booking.dto.response.PagedResponse;
import com.cinehub.booking.entity.Booking;
import com.cinehub.booking.mapper.BookingMapper; // Import Mapper của bạn
import com.cinehub.booking.repository.BookingRepository;
import com.cinehub.booking.service.BookingStatisticService; // Interface service của bạn
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor // Lombok tự tạo constructor cho các field final
public class BookingStatisticImpl implements BookingStatisticService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    @Override
    public PagedResponse<BookingResponse> getBookingsByCriteria(
            BookingCriteria criteria,
            int page,
            int size,
            String sortBy,
            String sortType) {

        // 1. Xử lý Sorting
        Sort sort = sortType.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        // 2. Tạo Pageable
        Pageable pageable = PageRequest.of(page, size, sort);

        // 3. Gọi Custom Repository
        Page<Booking> bookingsPage = bookingRepository.searchWithCriteria(criteria, pageable);

        // 4. Map Entity sang Response DTO dùng BookingMapper
        List<BookingResponse> bookingResponses = bookingsPage.getContent().stream()
                .map(bookingMapper::toBookingResponse)
                .toList();

        // 5. Đóng gói vào PagedResponse
        return PagedResponse.<BookingResponse>builder()
                .data(bookingResponses)
                .page(bookingsPage.getNumber()) // Trang hiện tại (bắt đầu từ 0)
                .size(bookingsPage.getSize()) // Kích thước trang
                .totalElements(bookingsPage.getTotalElements()) // Tổng số bản ghi
                .totalPages(bookingsPage.getTotalPages()) // Tổng số trang
                .build();
    }
}