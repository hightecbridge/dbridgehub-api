package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
import java.time.LocalDate; import java.util.List;
@Data @Builder public class StudentResponse {
    private Long id;
    private String name, grade, status, classroomName;
    private Long classroomId;
    private LocalDate birthDate;
    private List<FeeResponse> fees;
}
