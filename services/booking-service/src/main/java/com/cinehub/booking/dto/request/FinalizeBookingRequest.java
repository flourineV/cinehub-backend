package com.cinehub.booking.dto.request;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class FinalizeBookingRequest {

        private List<CalculatedFnbItemDto> fnbItems;

        private String promotionCode;

        private String refundVoucherCode;

        private boolean useLoyaltyDiscount;

        @Data
        public static class CalculatedFnbItemDto {
                private UUID fnbItemId;
                private Integer quantity;
                private BigDecimal unitPrice;
                private BigDecimal totalFnbItemPrice;
        }
}
