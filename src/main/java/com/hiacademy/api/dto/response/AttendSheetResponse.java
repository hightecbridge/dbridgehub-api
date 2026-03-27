package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime; import java.util.List;
@Data @Builder public class AttendSheetResponse {
    private Long id, classroomId;
    private String classroomName, date;
    private List<AttendRecordResponse> records;
    private LocalDateTime createdAt;
}
