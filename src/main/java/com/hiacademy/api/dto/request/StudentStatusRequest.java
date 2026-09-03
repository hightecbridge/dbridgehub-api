package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentStatusRequest {
    /** 재원 | 휴원 | 퇴원 */
    @NotBlank private String status;
}
