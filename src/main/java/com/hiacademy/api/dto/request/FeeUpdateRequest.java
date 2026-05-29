package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FeeUpdateRequest {
    @NotBlank
    private String label;
    private boolean paid;
    private int yearMonth;
    /** yyyy-MM-dd, 완납 시 미입력이면 당일 */
    private String paidAt;
    private String paymentMethod;
}
