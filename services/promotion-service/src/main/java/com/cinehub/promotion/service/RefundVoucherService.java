package com.cinehub.promotion.service;

import com.cinehub.promotion.dto.request.RefundVoucherRequest;
import com.cinehub.promotion.dto.response.RefundVoucherResponse;
import com.cinehub.promotion.entity.RefundVoucher;
import com.cinehub.promotion.entity.RefundVoucher.RefundType;
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
        RefundType refundType = request.getRefundType() != null 
                ? request.getRefundType() 
                : RefundType.USER_CANCELLED;

        // Chỉ kiểm tra giới hạn cho USER_CANCELLED (user tự hủy)
        // SYSTEM_REFUND không bị giới hạn
        if (refundType == RefundType.USER_CANCELLED) {
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

            long countThisMonth = refundVoucherRepository.countByUserIdAndRefundTypeAndCreatedAtBetween(
                    request.getUserId(),
                    RefundType.USER_CANCELLED,
                    startOfMonth,
                    endOfMonth);

            if (countThisMonth >= 1) {
                throw new IllegalStateException("❌ Bạn chỉ được tự hủy vé tối đa 1 lần mỗi tháng.");
            }
        }

        // Tạo voucher mới
        String code = generateVoucherCode();

        RefundVoucher voucher = RefundVoucher.builder()
                .userId(request.getUserId())
                .code(code)
                .value(request.getValue())
                .isUsed(false)
                .refundType(refundType)
                .createdAt(LocalDateTime.now())
                .expiredAt(request.getExpiredAt() != null
                        ? request.getExpiredAt()
                        : LocalDateTime.now().plusMonths(2))
                .build();

        refundVoucherRepository.save(voucher);
        log.info("Created refund voucher {} for user {} | type={}", code, request.getUserId(), refundType);

        return mapToResponse(voucher);
    }

    public List<RefundVoucherResponse> getAllVouchers() {
        return refundVoucherRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RefundVoucherResponse> getVouchersByUser(UUID userId) {
        return refundVoucherRepository.findByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<RefundVoucherResponse> getAvailableVouchersByUser(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        return refundVoucherRepository.findByUserId(userId)
                .stream()
                .filter(v -> !v.getIsUsed())
                .filter(v -> v.getExpiredAt().isAfter(now))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RefundVoucherResponse getVoucherByCode(String code) {
        RefundVoucher voucher = refundVoucherRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại: " + code));
        return mapToResponse(voucher);
    }

    public RefundVoucherResponse markAsUsed(String code) {
        RefundVoucher voucher = refundVoucherRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại"));

        if (voucher.getIsUsed()) {
            throw new RuntimeException("Voucher đã được sử dụng");
        }

        if (voucher.getExpiredAt() != null && voucher.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Voucher đã hết hạn");
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
                .refundType(v.getRefundType() != null ? v.getRefundType() : RefundType.USER_CANCELLED)
                .createdAt(v.getCreatedAt())
                .expiredAt(v.getExpiredAt())
                .build();
    }
}
