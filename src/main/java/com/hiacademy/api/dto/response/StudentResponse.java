package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
import java.time.LocalDate; import java.util.List;
@Data @Builder public class StudentResponse {
    private Long id;
    private String name, grade, status, classroomName, phone;
    private String parentName, parentPhone;
    private String badgeColor, badgeTextColor;
    private Boolean kakaoLinked;
    private Long classroomId;
    private LocalDate birthDate;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime withdrawnAt;
    private List<FeeResponse> fees;
}
