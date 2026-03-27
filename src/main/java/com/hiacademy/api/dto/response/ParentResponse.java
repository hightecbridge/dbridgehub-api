package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime; import java.util.List;
@Data @Builder public class ParentResponse {
    private Long id;
    private String name, phone, badgeColor, badgeTextColor;
    private boolean kakaoLinked;
    private LocalDateTime createdAt;
    private List<StudentResponse> students;
}
