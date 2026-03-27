package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class AdminSignupRequest {
    @NotBlank @Email private String email;
    @NotBlank @Size(min=4) private String password;
    @NotBlank private String name;
    private String phone;
    @NotBlank private String academyName;
    private String academyAddress;
    private String academyDesc;
}
