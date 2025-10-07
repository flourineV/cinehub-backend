package com.cinehub.pricing.dto.request;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class PricingRequest {
    private List<UUID> itemIds;     // danh sách id combo hoặc sản phẩm
    private String promoCode;       // mã khuyến mãi (nếu có)
}
