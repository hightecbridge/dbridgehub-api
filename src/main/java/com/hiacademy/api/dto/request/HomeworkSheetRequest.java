package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.*; import lombok.Data;
import java.util.List;
@Data public class HomeworkSheetRequest {
    @NotBlank private String date;
    @NotBlank private String title;
    @NotNull  private List<HomeworkRecordItem> records;
    @Data public static class HomeworkRecordItem {
        private Long studentId;
        private boolean done;
        private String comment;
    }
}
