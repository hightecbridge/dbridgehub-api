package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data public class ParentLoginRequest {
    @NotBlank private String phone;
    @NotBlank private String password;
}
