package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HomepageParentInquiryRequest {
    @NotBlank(message = "이름은 필수입니다.")
    private String name;
    @NotBlank(message = "연락처는 필수입니다.")
    private String phone;
    private String childName;
    private String message;
    private String source;
}
