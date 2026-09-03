package com.hiacademy.api.dto.request;

import lombok.Data;

@Data
public class BillingPointChargeRequest {
    /** 적립 포인트(결제 원화와 1:1, VAT 포함). 허용: 5000, 10000, 20000, 30000 */
    private int points;
    private String orderId;
    /** 토스페이먼츠 결제 인증 후 발급되는 키. 승인 API 호출에 필요 */
    private String paymentKey;
}
