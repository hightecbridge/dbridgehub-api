package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime; import java.util.List;
@Data @Builder public class HomeworkSheetResponse {
    private Long id, classroomId;
    private String classroomName, date, title;
    private List<HomeworkRecordResponse> records;
    private LocalDateTime createdAt;
}
