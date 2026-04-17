package com.hiacademy.api.dto.request;

import lombok.Data;

/** 구독 결제 반영 시 선택한 요금제·결제 주기(이용 기간 연장에 사용). */
@Data
public class BillingSubscribeRequest {
    private String planId;
    /** MONTHLY | YEARLY */
    private String billingCycle;
    /** 토스 결제 주문 ID (이력용) */
    private String orderId;
    /** 실제 결제 금액 원(부가세 포함). 이력·통계용 */
    private Long paidAmountKrw;
}
