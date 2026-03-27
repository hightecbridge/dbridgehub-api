package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class ConsultRequest {
    @NotNull  private Long studentId;
    @NotBlank private String date;
    private String time, content;
}
