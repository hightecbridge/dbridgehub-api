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
    private boolean paymentRequired;
    private String billingStatus;
    private int smsPoints;
    private int smsCostGeneral;
    private int smsCostPaymentNudge;
    private long monthlyPriceKrw;
}
