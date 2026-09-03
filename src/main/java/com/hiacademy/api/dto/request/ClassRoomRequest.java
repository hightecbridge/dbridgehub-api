package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class ClassRoomRequest {
    @NotBlank private String name;
    @NotBlank private String subject;
    private String teacher;
    private Long teacherUserId;
    private String schedule, color, textColor;
    @Min(1) private int capacity;
    @Min(0) private int tuitionFee;
    @Min(0) private int bookFee;
}
