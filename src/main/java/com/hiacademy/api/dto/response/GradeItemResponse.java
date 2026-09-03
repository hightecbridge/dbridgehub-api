package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class GradeItemResponse {
    private Long examId;
    private String title;
    private String date;
    private String subject;
    private String kind;
    private String kindLabel;
    private String classroomName;
    private Double score;
    private int maxScore;
    private Double classAvg;
    private Integer rank;
    private Integer rankedCount;
    /** 직전 시험 대비 점수 차이. 이전이 없으면 null */
    private Double delta;
    /** 내 점수 - 반/시험 평균. 학부모 화면용 */
    private Double vsClassAvg;
    private String comment;
    private List<ExamScoreSectionResponse> sectionScores;
}
