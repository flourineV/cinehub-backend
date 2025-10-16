package com.cinehub.booking.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Data; // Import Lombok @Data để tự động tạo getter/setter/toString/hashCode/equals

// SỬA ĐỔI: Chuyển đổi record FinalizeBookingRequest thành public class
@Data
public class FinalizeBookingRequest {

        // Danh sách các mục F&B đã được tính toán (hoặc sẽ được tính)
        private List<CalculatedFnbItemDto> fnbItems;

        // Mã khuyến mãi
        private String promotionCode;

        // SỬA ĐỔI: Chuyển đổi record CalculatedFnbItemDto thành public class lồng
        // (nested class)
        @Data
        public static class CalculatedFnbItemDto {
                // Thông tin cơ bản từ request
                private UUID fnbItemId;
                private Integer quantity;

                // Giá đơn vị (Unit Price) được tính/tra cứu từ service FNB
                private BigDecimal unitPrice;

                // Tổng giá của mục này (quantity * unitPrice)
                private BigDecimal totalFnbItemPrice;
        }
}