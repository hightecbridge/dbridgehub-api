package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class AdminLoginRequest {
    @NotBlank private String email;
    @NotBlank private String password;
}
