package com.cinehub.booking.dto.external;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class FnbItemDto {

    @NotNull
    private UUID fnbItemId; // ID của món F&B (ví dụ: bắp phô mai lớn)

    @Min(1)
    private int quantity; // Số lượng món F&B
}