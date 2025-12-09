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
    private String allowedDaysOfWeek;

    @Column(name = "allowed_days_of_month", length = 100)
    private String allowedDaysOfMonth;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "promo_display_url", length = 500)
    private String promoDisplayUrl;

    public boolean isOneTimeUse() {
        if (promotionType == null) {
            return false;
        }
        return promotionType == PromotionType.FIRST_TIME;
    }

    public enum PromotionType {
        GENERAL,
        WEEKEND,
        FIRST_TIME,
    }

    public enum UsageTimeRestriction {
        NONE,
        WEEKEND_ONLY,
        WEEKDAY_ONLY,
        MONTH_START,
        MONTH_END,
        CUSTOM_DAYS
    }
}