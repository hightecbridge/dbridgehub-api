package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ExamClassProgressResponse {
    private Long classroomId;
    private String classroomName;
    private int entered;
    private int total;
    private boolean complete;
    private String status;
}
