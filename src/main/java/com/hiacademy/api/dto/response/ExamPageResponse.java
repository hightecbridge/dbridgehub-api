package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExamPageResponse {
    private List<ExamResponse> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
