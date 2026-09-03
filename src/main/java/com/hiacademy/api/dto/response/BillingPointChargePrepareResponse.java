package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BillingPointChargePrepareResponse {
    private String orderId;
    private long amount;
    private String orderName;
}
