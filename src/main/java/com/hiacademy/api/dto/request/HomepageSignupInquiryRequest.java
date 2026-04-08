package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HomepageSignupInquiryRequest {
    @NotBlank(message = "학원명은 필수입니다.")
    private String academyName;
    @NotBlank(message = "이름은 필수입니다.")
    private String name;
    @NotBlank(message = "연락처는 필수입니다.")
    private String phone;
    private String studentCount;
    private String message;
    private String source;
}
