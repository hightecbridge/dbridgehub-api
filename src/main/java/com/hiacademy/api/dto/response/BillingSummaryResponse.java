package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BillingSummaryResponse {
    private LocalDateTime trialEndsAt;
    private int trialDaysRemaining;
    private boolean trialActive;
    /** ?좊즺 援щ룆 留뚮즺 ?쒓컖 諛??⑥? ?쇱닔(ACTIVE ?닿퀬 援щ룆 醫낅즺?쇱씠 ?덉쓣 ??. */
    private LocalDateTime subscriptionEndsAt;
    private int subscriptionDaysRemaining;
    private boolean paymentRequired;
    private String billingStatus;
    private int smsPoints;
    private int smsCostGeneral;
    private int smsCostKakaoAlimtalk;
    private int smsCostSms;
    private int smsCostLms;
    private int smsCostMms;
    private int smsCostPaymentNudge;
    private long monthlyPriceKrw;
    /** 援щ룆 ?붽툑??(standard|premium|enterprise) */
    private String billingPlanId;
    /** ?댁썝 ?쒖쇅 ?숈깮 ??*/
    private long studentCount;
    /** ?깅줉 媛???곹븳. -1 ?대㈃ 臾댁젣???뷀꽣?꾨씪?댁쫰) */
    private int studentLimit;
    /** 토스 빌링 등록 후 매월 자동결제 사용 여부 */
    private boolean autoBillingEnabled;
    /** 빌링키(정기결제 카드) 발급·등록 시각 */
    private LocalDateTime billingKeyIssuedAt;
}
