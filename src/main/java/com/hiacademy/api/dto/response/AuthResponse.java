package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder public class AuthResponse {
    private String token, type, email, name, role, phone, profileImageBase64;
    private LocalDateTime createdAt;
    private Long id;
    private AcademyInfo academy;
}
