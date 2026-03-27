package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.*; import lombok.Data;
import java.time.LocalDate;
@Data public class StudentRequest {
    @NotBlank private String name;
    @NotBlank private String grade;
    private LocalDate birthDate;
    private String status;
    @NotNull private Long classroomId;
}
