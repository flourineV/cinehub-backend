package com.cinehub.fnb.dto.request;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class FnbCalculationRequest {

    // Danh sách các mục F&B và số lượng tương ứng được chọn
    @Valid
    @NotEmpty
    private List<FnbItemDto> selectedFnbItems;
}