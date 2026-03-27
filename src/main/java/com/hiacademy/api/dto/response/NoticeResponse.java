package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime; import java.util.List;
@Data @Builder public class NoticeResponse {
    private Long id;
    private String title, body, imageUrl, date;
    private List<String> targets;
    private LocalDateTime createdAt;
}
