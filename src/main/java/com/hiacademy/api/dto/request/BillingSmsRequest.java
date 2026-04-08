package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** GENERAL: 일반 문자, PAYMENT_NUDGE: 결제 안내 문자 */
@Data
public class BillingSmsRequest {
    @NotBlank
    private String type;
    /** 수신 번호(마스킹 로그용) 또는 메모 */
    private String targetPhone;
}
