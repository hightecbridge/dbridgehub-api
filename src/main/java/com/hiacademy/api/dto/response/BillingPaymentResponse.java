package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BillingPaymentResponse {
    private Long id;
    /** SUBSCRIPTION | POINT_CHARGE */
    private String paymentType;
    private long amountKrw;
    private String orderId;
    private String summary;
    private LocalDateTime paidAt;
}
