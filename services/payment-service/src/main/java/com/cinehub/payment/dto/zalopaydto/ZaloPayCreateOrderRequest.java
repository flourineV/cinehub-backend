package com.cinehub.payment.dto.zalopaydto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ZaloPayCreateOrderRequest {
    @JsonProperty("app_id")
    private int appId;

    @JsonProperty("app_user")
    private String appUser;

    @JsonProperty("app_time")
    private long appTime;

    @JsonProperty("amount")
    private long amount;

    @JsonProperty("app_trans_id")
    private String appTransId;

    @JsonProperty("bank_code")
    private String bankCode;

    @JsonProperty("embed_data")
    private String embedData;

    @JsonProperty("item")
    private String item;

    @JsonProperty("callback_url")
    private String callbackUrl;

    @JsonProperty("description")
    private String description;

    @JsonProperty("mac")
    private String mac;
}