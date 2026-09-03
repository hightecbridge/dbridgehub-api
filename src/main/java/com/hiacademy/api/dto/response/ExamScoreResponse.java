package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class ExamScoreResponse {
    private Long studentId;
    private String studentName;
    private Long classroomId;
    private String classroomName;
    private Double score;
    private String comment;
    private Integer rank;
    private Integer rankedCount;
    private List<ExamScoreSectionResponse> sectionScores;
}
