package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ExamScoreSectionResponse {
    private Long sectionId;
    private String name;
    private int maxScore;
    private Double score;
    private Double percent;
}
