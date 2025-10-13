package com.cinehub.fnb.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.util.UUID;

@Data
public class FnbItemDto {

    @NotNull
    private UUID fnbItemId;

    @NotNull
    @Min(value = 1)
    private Integer quantity;
}