package com.cinehub.payment.service;

import com.cinehub.payment.dto.response.PaymentStatsResponse;
import com.cinehub.payment.dto.response.RevenueStatsResponse;
import com.cinehub.payment.entity.PaymentStatus;
import com.cinehub.payment.entity.PaymentTransaction;
import com.cinehub.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentStatsService {

    private final PaymentRepository paymentRepository;

    public PaymentStatsResponse getOverview() {
        long total = paymentRepository.count();
        List<PaymentTransaction> allPayments = paymentRepository.findAll();

        long successful = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .count();
        long failed = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.FAILED)
                .count();
        long pending = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .count();

        BigDecimal totalRevenue = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(PaymentTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PaymentStatsResponse.builder()
                .totalPayments(total)
                .successfulPayments(successful)
                .failedPayments(failed)
                .pendingPayments(pending)
                .totalRevenue(totalRevenue)
                .build();
    }

    public List<RevenueStatsResponse> getRevenueStats(Integer year, Integer month) {
        List<PaymentTransaction> payments = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .filter(p -> filterByYear(p, year))
                .filter(p -> filterByMonth(p, month))
                .collect(Collectors.toList());

        // Group by year and month
        Map<String, List<PaymentTransaction>> grouped = payments.stream()
                .collect(Collectors.groupingBy(p -> {
                    LocalDateTime created = p.getCreatedAt();
                    if (month != null) {
                        return created.getYear() + "-" + created.getMonthValue();
                    }
                    return String.valueOf(created.getYear());
                }));

        return grouped.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    List<PaymentTransaction> groupPayments = entry.getValue();

                    String[] parts = key.split("-");
                    int yr = Integer.parseInt(parts[0]);
                    Integer mn = parts.length > 1 ? Integer.parseInt(parts[1]) : null;

                    BigDecimal revenue = groupPayments.stream()
                            .map(PaymentTransaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    long count = groupPayments.size();
                    BigDecimal avgValue = count > 0
                            ? revenue.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return RevenueStatsResponse.builder()
                            .year(yr)
                            .month(mn)
                            .totalRevenue(revenue)
                            .totalPayments(count)
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

    private boolean filterByYear(PaymentTransaction payment, Integer year) {
        if (year == null)
            return true;
        return payment.getCreatedAt().getYear() == year;
    }

    private boolean filterByMonth(PaymentTransaction payment, Integer month) {
        if (month == null)
            return true;
        return payment.getCreatedAt().getMonthValue() == month;
    }
}
