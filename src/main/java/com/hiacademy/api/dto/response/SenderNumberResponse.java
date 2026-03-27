package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SenderNumberResponse {
    private Long id;
    private String label;
    private String number;
    private boolean isDefault;
}

