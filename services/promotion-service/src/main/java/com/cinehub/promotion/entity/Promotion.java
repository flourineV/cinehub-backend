package com.cinehub.promotion.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "promotion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "promotion_type", nullable = false, length = 30)
    @Builder.Default
    private PromotionType promotionType = PromotionType.GENERAL;

    // Giả định loại chiết khấu là ENUM (có thể dùng String)
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_time_restriction", length = 30)
    @Builder.Default
    private UsageTimeRestriction usageTimeRestriction = UsageTimeRestriction.NONE;

    @Column(name = "allowed_days_of_week", length = 50)
    private String allowedDaysOfWeek; // e.g., "SATURDAY,SUNDAY" for weekends

    @Column(name = "allowed_days_of_month", length = 100)
    private String allowedDaysOfMonth; // e.g., "1,2,3" for first 3 days of month

    @Column(name = "description", length = 500)
    private String description;

    /**
     * Check if this promotion is one-time-use based on promotion type
     */
    public boolean isOneTimeUse() {
        if (promotionType == null) {
            return false;
        }
        return promotionType == PromotionType.FIRST_TIME || 
               promotionType == PromotionType.BIRTHDAY ||
               promotionType == PromotionType.REFERRAL;
    }

    public enum PromotionType {
        GENERAL,        // Khuyến mãi chung - có thể dùng nhiều lần
        WEEKEND,        // Khuyến mãi cuối tuần - có thể dùng nhiều lần
        HOLIDAY,        // Khuyến mãi ngày lễ - có thể dùng nhiều lần
        BIRTHDAY,       // Khuyến mãi sinh nhật - chỉ dùng 1 lần
        LOYALTY,        // Khuyến mãi thành viên thân thiết - có thể dùng nhiều lần
        FIRST_TIME,     // Khuyến mãi lần đầu - chỉ dùng 1 lần
        SEASONAL,       // Khuyến mãi theo mùa - có thể dùng nhiều lần
        FLASH_SALE,     // Khuyến mãi giờ vàng - có thể dùng nhiều lần
        REFERRAL        // Khuyến mãi giới thiệu bạn bè - chỉ dùng 1 lần
    }

    public enum UsageTimeRestriction {
        NONE,           // Không giới hạn
        WEEKEND_ONLY,   // Chỉ cuối tuần (Thứ 7, CN)
        WEEKDAY_ONLY,   // Chỉ ngày thường (T2-T6)
        MONTH_START,    // Đầu tháng (ngày 1-5)
        MONTH_END,      // Cuối tháng (5 ngày cuối)
        CUSTOM_DAYS     // Tùy chỉnh theo allowedDaysOfWeek hoặc allowedDaysOfMonth
    }
}