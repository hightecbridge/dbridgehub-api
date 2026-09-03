package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class GradeHistoryResponse {
    private Long studentId;
    private String studentName;
    private String classroomName;
    private Integer examCount;
    private Double average;
    private Double latestDelta;
    private List<GradeItemResponse> items;
}
