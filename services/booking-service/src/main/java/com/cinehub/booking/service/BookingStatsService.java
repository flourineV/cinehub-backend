package com.cinehub.booking.service;

import com.cinehub.booking.adapter.client.ShowtimeClient;
import com.cinehub.booking.dto.external.ShowtimeDetailResponse;
import com.cinehub.booking.dto.response.BookingStatsResponse;
import com.cinehub.booking.dto.response.RevenueStatsResponse;
import com.cinehub.booking.entity.Booking;
import com.cinehub.booking.entity.BookingStatus;
import com.cinehub.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingStatsService {

    private final BookingRepository bookingRepository;
    private final ShowtimeClient showtimeClient;

    public BookingStatsResponse getOverview(UUID theaterId) {
        List<Booking> allBookings;

        if (theaterId != null) {
            // Filter by theater: get showtimes of that theater first
            List<ShowtimeDetailResponse> showtimes = showtimeClient.getShowtimesByFilter(
                    null, theaterId, null, null);
            Set<UUID> validShowtimeIds = showtimes.stream()
                    .map(ShowtimeDetailResponse::getId)
                    .collect(Collectors.toSet());

            allBookings = bookingRepository.findAll().stream()
                    .filter(b -> validShowtimeIds.contains(b.getShowtimeId()))
                    .collect(Collectors.toList());
        } else {
            allBookings = bookingRepository.findAll();
        }

        long total = allBookings.size();
        long confirmed = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();
        long cancelled = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED || b.getStatus() == BookingStatus.REFUNDED)
                .count();
        long pending = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .count();

        BigDecimal totalRevenue = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return BookingStatsResponse.builder()
                .totalBookings(total)
                .confirmedBookings(confirmed)
                .cancelledBookings(cancelled)
                .pendingBookings(pending)
                .totalRevenue(totalRevenue)
                .build();
    }

    public List<RevenueStatsResponse> getRevenueStats(Integer year, Integer month, UUID theaterId, UUID provinceId) {
        // Bước 1: Xác định khoảng thời gian
        LocalDate startDate = null;
        LocalDate endDate = null;

        if (year != null) {
            if (month != null) {
                startDate = LocalDate.of(year, month, 1);
                endDate = startDate.plusMonths(1).minusDays(1);
            } else {
                startDate = LocalDate.of(year, 1, 1);
                endDate = LocalDate.of(year, 12, 31);
            }
        }

        // Bước 2: Lấy danh sách showtimeId từ showtime service
        final Set<UUID> validShowtimeIds;

        if (theaterId != null || provinceId != null) {
            List<ShowtimeDetailResponse> showtimes = showtimeClient.getShowtimesByFilter(
                    provinceId, theaterId, startDate, endDate);
            validShowtimeIds = showtimes.stream()
                    .map(ShowtimeDetailResponse::getId)
                    .collect(Collectors.toSet());

            // Nếu không có showtime nào, return empty
            if (validShowtimeIds.isEmpty()) {
                return Collections.emptyList();
            }
        } else {
            validShowtimeIds = Collections.emptySet();
        }

        // Bước 3: Lấy bookings
        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> filterByYear(b, year))
                .filter(b -> filterByMonth(b, month))
                .filter(b -> validShowtimeIds.isEmpty() || validShowtimeIds.contains(b.getShowtimeId()))
                .collect(Collectors.toList());

        // Bước 4: Group by year and month
        Map<String, List<Booking>> grouped = bookings.stream()
                .collect(Collectors.groupingBy(b -> {
                    LocalDateTime created = b.getCreatedAt();
                    if (month != null) {
                        return created.getYear() + "-" + created.getMonthValue();
                    }
                    return String.valueOf(created.getYear());
                }));

        // Bước 5: Tạo response
        return grouped.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    List<Booking> groupBookings = entry.getValue();

                    String[] parts = key.split("-");
                    int yr = Integer.parseInt(parts[0]);
                    Integer mn = parts.length > 1 ? Integer.parseInt(parts[1]) : null;

                    BigDecimal revenue = groupBookings.stream()
                            .map(Booking::getFinalPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    long count = groupBookings.size();
                    BigDecimal avgValue = count > 0
                            ? revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return RevenueStatsResponse.builder()
                            .year(yr)
                            .month(mn)
                            .totalRevenue(revenue)
                            .totalBookings(count)
                            .averageOrderValue(avgValue)
                            .build();
                })
                .sorted((a, b) -> {
                    if (a.getYear() != b.getYear()) {
                        return Integer.compare(a.getYear(), b.getYear());
                    }
                    if (a.getMonth() == null)
                        return -1;
                    if (b.getMonth() == null)
                        return 1;
                    return Integer.compare(a.getMonth(), b.getMonth());
                })
                .collect(Collectors.toList());
    }

    private boolean filterByYear(Booking booking, Integer year) {
        if (year == null)
            return true;
        return booking.getCreatedAt().getYear() == year;
    }

    private boolean filterByMonth(Booking booking, Integer month) {
        if (month == null)
            return true;
        return booking.getCreatedAt().getMonthValue() == month;
    }
}
