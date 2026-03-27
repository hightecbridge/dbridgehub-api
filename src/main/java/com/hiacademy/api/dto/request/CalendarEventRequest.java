package com.hiacademy.api.dto.request;
import jakarta.validation.constraints.*; import lombok.Data;
import java.util.List;
@Data public class CalendarEventRequest {
    @NotBlank private String title;
    @NotBlank private String date;
    private String endDate, category, description, color;
    @NotNull private List<String> targets;
    private boolean allDay;
}
