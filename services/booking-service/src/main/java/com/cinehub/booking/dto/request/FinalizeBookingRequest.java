package com.cinehub.booking.dto.request;

import com.cinehub.booking.dto.external.FnbItemDto;
import lombok.Data;
import java.util.List;

@Data
public class FinalizeBookingRequest {

    // Danh sách các món F&B đã chọn (cần quantity và fnbItemId)
    private List<FnbItemDto> fnbItems;

    // Mã khuyến mãi người dùng nhập
    private String discountCode;

    // Có thể bao gồm bookingId nếu không dùng nó trong URL
    // private UUID bookingId;
}