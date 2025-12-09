package com.cinehub.promotion.dto.request;

import com.cinehub.promotion.entity.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PromotionRequest {

    @NotBlank(message = "Mã khuyến mãi không được để trống.")
    @Size(max = 50, message = "Mã khuyến mãi không được quá 50 ký tự.")
    private String code;

    private com.cinehub.promotion.entity.Promotion.PromotionType promotionType;

    @NotNull(message = "Loại chiết khấu không được để trống.")
    private DiscountType discountType;

    @NotNull(message = "Giá trị chiết khấu không được để trống.")
    @DecimalMin(value = "0.00", message = "Giá trị chiết khấu phải lớn hơn hoặc bằng 0.")
    private BigDecimal discountValue;

    @NotNull(message = "Ngày bắt đầu không được để trống.")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống.")
    private LocalDateTime endDate;

    private Boolean isActive = true; // Mặc định là True

    private com.cinehub.promotion.entity.Promotion.UsageTimeRestriction usageTimeRestriction;

    private String allowedDaysOfWeek; // e.g., "SATURDAY,SUNDAY"

    private String allowedDaysOfMonth; // e.g., "1,2,3,15,20"

    @Size(max = 500, message = "Mô tả không được quá 500 ký tự.")
    private String description;

    @Size(max = 500, message = "URL ảnh không được quá 500 ký tự.")
    private String promoDisplayUrl;
}