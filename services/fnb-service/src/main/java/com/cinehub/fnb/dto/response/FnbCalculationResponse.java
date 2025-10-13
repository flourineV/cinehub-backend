package com.cinehub.fnb.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class FnbCalculationResponse {

    // Tổng giá trị F&B đã tính
    private BigDecimal totalFnbPrice;

    // (Có thể thêm chi tiết từng mục nếu cần cho hóa đơn)
}