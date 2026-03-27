package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
@Data @Builder public class AttendRecordResponse {
    private Long id, studentId;
    private String studentName, status, note, date;
}
