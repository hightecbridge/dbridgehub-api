package com.hiacademy.api.dto.response;
import lombok.Builder; import lombok.Data;
import java.time.LocalDateTime;
@Data @Builder public class ClassRoomResponse {
    private Long id;
    private String name, subject, teacher, schedule, color, textColor;
    private int capacity, tuitionFee, bookFee;
    private LocalDateTime createdAt;
}
