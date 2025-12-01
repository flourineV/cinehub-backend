package com.cinehub.payment.dto.zalopaydto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZaloCallbackDTO {

    // ZaloPay trả về một chuỗi JSON (String) chứa toàn bộ thông tin đơn hàng
    // Ví dụ: "{\"app_id\": 2553, \"app_trans_id\": \"...\", \"amount\": 50000,
    // ...}"
    @JsonProperty("data")
    private String data;

    // Chữ ký checksum để bạn dùng key2 verify xem có đúng Zalo gửi không
    @JsonProperty("mac")
    private String mac;

    // Loại callback (thường là 1 hoặc 2, không quá quan trọng với luồng cơ bản)
    @JsonProperty("type")
    private int type;
}