package com.hiacademy.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ExamRequest {
    @NotBlank private String title;
    @NotBlank private String date;
    private String subject;
    @Min(1) @Max(1000)
    private Integer maxScore;
    /** ALL = 전체시험, CLASS = 반별테스트 */
    private String kind;
    /** 전체시험 대상 학년. 비우면 전학년 */
    private String targetGrade;
    private List<Long> classroomIds;
    @Valid
    private List<Section> sections;

    @Data
    public static class Section {
        private Long id;
        private String name;
        @Min(1) @Max(1000)
        private Integer maxScore;
        @Min(0) @Max(1000)
        private Integer weight;
    }
}
