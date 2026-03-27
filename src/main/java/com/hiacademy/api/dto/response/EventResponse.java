package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime; import java.util.List;
@Data @Builder public class EventResponse {
    private Long id;
    private String title, date, endDate, category, description, color;
    private List<String> targets;
    private boolean allDay;
    private LocalDateTime createdAt;
}
