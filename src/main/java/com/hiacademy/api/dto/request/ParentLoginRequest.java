package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data public class ParentLoginRequest {
    @NotBlank private String phone;
    @NotBlank private String password;
    /** 같은 번호가 여러 학원에 있을 때, 앱에서 고른 학원 id */
    private Long academyId;
}
