package com.cinehub.notification.dto.email;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PromotionEmailData {
    private final String code;
    private final String description;
    private final BigDecimal discountValue;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final boolean oneTimeUse;
    private final String discountTypeName;

    public PromotionEmailData(String code, String description, BigDecimal discountValue,
            LocalDateTime startDate, LocalDateTime endDate,
            boolean oneTimeUse, String discountTypeName) {
        this.code = code;
        this.description = description;
        this.discountValue = discountValue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.oneTimeUse = oneTimeUse;
        this.discountTypeName = discountTypeName;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public boolean isOneTimeUse() {
        return oneTimeUse;
    }

    public DiscountTypeData getDiscountType() {
        return new DiscountTypeData(discountTypeName);
    }

    public static class DiscountTypeData {
        private final String name;

        public DiscountTypeData(String name) {
            this.name = name;
        }

        public String name() {
            return name;
        }
    }
}
