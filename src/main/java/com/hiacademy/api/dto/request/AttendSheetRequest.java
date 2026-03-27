package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.*; import lombok.Data;
import java.util.List;
@Data public class AttendSheetRequest {
    @NotBlank private String date;
    @NotNull  private List<AttendRecordItem> records;
    @Data public static class AttendRecordItem {
        private Long studentId;
        private String status, note;
    }
}
