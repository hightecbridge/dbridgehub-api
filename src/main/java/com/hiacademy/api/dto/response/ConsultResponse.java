package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data @Builder
public class ConsultResponse {
    private Long id;
    private String kind;
    private Long studentId;
    private String studentName;
    private String studentStatus;
    private String classroomName;
    private Long teacherUserId;
    private String teacherName;
    private String date;
    private String time;
    private String status;
    private String content;
    private String prospectName;
    private String prospectPhone;
    private String prospectGrade;
    private String prospectParentName;
    private LocalDateTime createdAt;
}
