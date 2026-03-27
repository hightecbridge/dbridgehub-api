package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class ClassRoomRequest {
    @NotBlank private String name;
    @NotBlank private String subject;
    @NotBlank private String teacher;
    private String schedule, color, textColor;
    @Min(1) private int capacity;
    @Min(0) private int tuitionFee;
    @Min(0) private int bookFee;
}
