package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class ClassGradeStatsResponse {
    private Long classroomId;
    private String classroomName;
    private Integer examCount;
    private Double overallAverage;
    private List<ExamTrendPoint> examTrend;
    private List<StudentRankRow> ranking;

    @Data @Builder
    public static class ExamTrendPoint {
        private Long examId;
        private String title;
        private String date;
        private Double average;
        private Integer participantCount;
        private Double high;
        private Double low;
    }

    @Data @Builder
    public static class StudentRankRow {
        private Long studentId;
        private String studentName;
        private Integer examCount;
        private Double average;
        private Double latestScore;
        private Double latestDelta;
        private Integer rank;
    }
}
