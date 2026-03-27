package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SenderNumberRequest {
    @NotBlank
    private String label;

    @NotBlank
    private String number;
}

