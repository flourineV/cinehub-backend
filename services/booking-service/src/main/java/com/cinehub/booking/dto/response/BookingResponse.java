package com.cinehub.booking.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
    private UUID bookingId;
    private UUID userId;
    private UUID showtimeId;

    private String status;

    // ✅ THÊM 2 TRƯỜNG MỚI ĐỂ PHẢN ÁNH THÔNG TIN TỪ ENTITY
    private BigDecimal totalPrice; // Tổng tiền trước giảm giá (Ghế + F&B)
    private BigDecimal discountAmount; // Số tiền giảm giá thực tế
    private BigDecimal finalPrice; // Tổng tiền cuối cùng phải trả

    // ✅ THÊM: Phương thức thanh toán
    private String paymentMethod;

    // ✅ THÊM: Mã giao dịch
    private String transactionId;

    // Tùy chọn: Thêm FNB và Promotion Response để chi tiết hơn
    // private List<BookingFnbResponse> fnbItems;
    // private PromotionResponse promotion;

    // Hiện tại giữ nguyên list seats để đơn giản
    private List<BookingSeatResponse> seats;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}