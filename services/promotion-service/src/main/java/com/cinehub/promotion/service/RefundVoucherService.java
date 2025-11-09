package com.cinehub.promotion.service;

import com.cinehub.promotion.dto.request.RefundVoucherRequest;
import com.cinehub.promotion.dto.response.RefundVoucherResponse;
import com.cinehub.promotion.entity.RefundVoucher;
import com.cinehub.promotion.repository.RefundVoucherRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundVoucherService {

    private final RefundVoucherRepository refundVoucherRepository;

    public RefundVoucherResponse createRefundVoucher(RefundVoucherRequest request) {
        // Kiểm tra số voucher đã tạo trong tháng hiện tại
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

        long countThisMonth = refundVoucherRepository.countByUserIdAndCreatedAtBetween(
                request.getUserId(),
                startOfMonth,
                endOfMonth);

        if (countThisMonth >= 2) {
            throw new IllegalStateException("❌ Bạn chỉ được hoàn vé tối đa 2 lần mỗi tháng.");
        }

        // Tạo voucher mới như cũ
        String code = generateVoucherCode();

        RefundVoucher voucher = RefundVoucher.builder()
                .userId(request.getUserId())
                .code(code)
                .value(request.getValue())
                .isUsed(false)
                .createdAt(LocalDateTime.now())
                .expiredAt(request.getExpiredAt() != null
                        ? request.getExpiredAt()
                        : LocalDateTime.now().plusMonths(6))
                .build();

        refundVoucherRepository.save(voucher);
        log.info("Created refund voucher {} for user {}", code, request.getUserId());

        return mapToResponse(voucher);
    }

    public List<RefundVoucherResponse> getAllVouchers() {
        return refundVoucherRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RefundVoucherResponse> getVouchersByUser(UUID userId) {
        return refundVoucherRepository.findAll()
                .stream()
                .filter(v -> v.getUserId().equals(userId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RefundVoucherResponse markAsUsed(String code) {
        RefundVoucher voucher = refundVoucherRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        if (voucher.getIsUsed()) {
            throw new RuntimeException("Voucher đã được sử dụng");
        }

        voucher.setIsUsed(true);
        refundVoucherRepository.save(voucher);

        log.info("Voucher {} marked as used", code);
        return mapToResponse(voucher);
    }

    private String generateVoucherCode() {
        String code;
        do {
            code = "VCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (refundVoucherRepository.existsByCode(code));
        return code;
    }

    private RefundVoucherResponse mapToResponse(RefundVoucher v) {
        return RefundVoucherResponse.builder()
                .id(v.getId())
                .code(v.getCode())
                .userId(v.getUserId())
                .value(v.getValue())
                .isUsed(v.getIsUsed())
                .createdAt(v.getCreatedAt())
                .expiredAt(v.getExpiredAt())
                .build();
    }
}
