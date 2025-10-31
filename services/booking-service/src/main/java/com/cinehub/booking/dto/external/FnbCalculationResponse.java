package com.cinehub.booking.dto.external;

import com.cinehub.booking.dto.request.FinalizeBookingRequest.CalculatedFnbItemDto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class FnbCalculationResponse {

    private BigDecimal totalFnbPrice;

    private List<CalculatedFnbItemDto> calculatedFnbItems;
}