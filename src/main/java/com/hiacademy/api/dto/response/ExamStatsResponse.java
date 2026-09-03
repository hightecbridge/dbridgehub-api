package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class ExamStatsResponse {
    private Long examId;
    private String title;
    private String date;
    private String subject;
    private String kind;
    private String kindLabel;
    private int maxScore;
    private Integer participantCount;
    private Integer totalStudents;
    private Double average;
    private Double high;
    private Double low;
    private Double median;
    private List<ClassBreakdown> classrooms;
    private List<DistributionBucket> distribution;
    private List<StudentRankRow> ranking;

    @Data @Builder
    public static class ClassBreakdown {
        private Long classroomId;
        private String classroomName;
        private Integer participantCount;
        private Integer totalStudents;
        private Double average;
        private Double high;
        private Double low;
    }

    @Data @Builder
    public static class DistributionBucket {
        private String label;
        private int count;
    }

    @Data @Builder
    public static class StudentRankRow {
        private Long studentId;
        private String studentName;
        private String classroomName;
        private Double score;
        private Integer rank;
        private Integer rankedCount;
    }
}
