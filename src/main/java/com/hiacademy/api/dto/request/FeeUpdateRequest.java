package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.NotBlank; import lombok.Data;
@Data public class FeeUpdateRequest {
    @NotBlank private String label;
    private boolean paid;
    private int yearMonth;
}
