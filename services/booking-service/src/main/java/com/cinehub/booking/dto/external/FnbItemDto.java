package com.cinehub.booking.dto.external;

import lombok.Data;
import java.util.UUID;

@Data
public class FnbItemDto {
    private UUID fnbItemId;
    private Integer quantity;
}