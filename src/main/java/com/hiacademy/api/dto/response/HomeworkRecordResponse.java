package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder public class HomeworkRecordResponse {
    private Long id, studentId;
    private String studentName, comment, date, title;
    private boolean done;
    private LocalDateTime updatedAt;
}
