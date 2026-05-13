package com.hiacademy.api.dto.request;

import lombok.Data;

/** 카드 등록 성공 후 빌링키 발급 + 정기결제 시작 요청 */
@Data
public class BillingAutoSubscribeRequest {
    private String planId;
    /** 토스 빌링 customerKey(가맹점 내부 사용자 식별키) */
    private String customerKey;
    /** 카드번호(숫자만, 최대 20자) */
    private String cardNumber;
    /** 카드 유효기간 연도(YY 또는 YYYY) */
    private String cardExpirationYear;
    /** 카드 유효기간 월(MM) */
    private String cardExpirationMonth;
    /** 카드 소유자 정보(생년월일 6자리 또는 사업자번호 10자리) */
    private String customerIdentityNumber;
    /** 카드 비밀번호 앞 2자리 */
    private String cardPassword;
    /** 구매자명(선택) */
    private String customerName;
    /** 구매자 이메일(선택) */
    private String customerEmail;
    /** 결제 이력용 주문번호 */
    private String orderId;
    /** 최초 청구 금액(VAT 포함) */
    private Long paidAmountKrw;
}
