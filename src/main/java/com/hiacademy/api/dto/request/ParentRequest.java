package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data public class ParentRequest {
    @NotBlank private String name;
    @NotBlank private String phone;
    private String badgeColor, badgeTextColor, loginPhone, loginPassword;
    private boolean kakaoLinked;
}
