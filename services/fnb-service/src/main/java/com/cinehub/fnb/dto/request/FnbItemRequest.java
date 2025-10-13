package com.cinehub.fnb.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

@Data
public class FnbItemRequest {

    @NotBlank(message = "Tên mục F&B không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Đơn giá không được để trống")
    @DecimalMin(value = "0.00", inclusive = false, message = "Đơn giá phải lớn hơn 0")
    private BigDecimal unitPrice;
}