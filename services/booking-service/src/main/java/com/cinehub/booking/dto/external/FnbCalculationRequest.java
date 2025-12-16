package com.cinehub.booking.dto.external;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

/**
 * DTO gửi từ BookingService sang FnbService để tính tổng giá F&B.
 * Giữ đúng cấu trúc mà FnbService đang nhận:
 * {
 * "selectedFnbItems": [ { fnbItemId, quantity } ]
 * }
 */
@Data
public class FnbCalculationRequest {

    @Valid
    @NotEmpty
    private List<FnbItemDto> selectedFnbItems;
}
