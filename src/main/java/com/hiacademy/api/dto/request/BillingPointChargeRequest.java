package com.hiacademy.api.dto.request;

import lombok.Data;

@Data
public class BillingPointChargeRequest {
    /** 적립 포인트(결제 원화와 1:1). 허용: 10000, 20000, 30000 */
    private int points;
    private String orderId;
}
