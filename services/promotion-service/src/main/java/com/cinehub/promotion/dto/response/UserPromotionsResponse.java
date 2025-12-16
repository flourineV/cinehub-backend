package com.cinehub.promotion.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserPromotionsResponse {
    private List<ApplicablePromotionResponse> applicable;
    private List<NotApplicablePromotionResponse> notApplicable;

    @Data
    @Builder
    public static class ApplicablePromotionResponse {
        private PromotionResponse promotion;
    }

    @Data
    @Builder
    public static class NotApplicablePromotionResponse {
        private PromotionResponse promotion;
    }
}
