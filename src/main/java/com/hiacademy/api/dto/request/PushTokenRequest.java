package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PushTokenRequest {
    @NotBlank(message = "expoPushToken은 필수입니다.")
    private String expoPushToken;
}
