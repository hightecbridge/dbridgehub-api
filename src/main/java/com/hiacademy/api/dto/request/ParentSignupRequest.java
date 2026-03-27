package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ParentSignupRequest {
    @NotNull private Long academyId;
    @NotBlank private String name;
    @NotBlank private String phone;
    @NotBlank private String password;
}

