package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeeResponse {
    private Long id;
    private String label;
    private int amount;
    private int yearMonth;
    private boolean paid;
    /** yyyy-MM-dd */
    private String paidAt;
    private String paymentMethod;
}
