package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class ExamSectionResponse {
    private Long id;
    private String name;
    private int maxScore;
    private int weight;
    private int sortOrder;
}
