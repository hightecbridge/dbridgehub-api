package com.hiacademy.api.dto.response;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
@Data @Builder public class AuthResponse {
    private String token, type, email, name, role, phone, profileImageBase64;
    private LocalDateTime createdAt;
    private Long id;
    private AcademyInfo academy;
    /** true 이면 token 없이 academies 목록만 내려 학원 선택을 요청한다. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean needsAcademySelection;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ParentLoginAcademyOption> academies;
}
