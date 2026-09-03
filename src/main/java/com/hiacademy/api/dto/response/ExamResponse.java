package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class ExamResponse {
    private Long id;
    private Long classroomId;
    private String classroomName;
    private List<ExamClassroomResponse> classrooms;
    private String kind;
    private String kindLabel;
    private String targetGrade;
    private String targetLabel;
    private String progressLabel;
    private Integer completedClasses;
    private Integer totalClasses;
    private List<ExamClassProgressResponse> classProgress;
    private String status;
    private String statusLabel;
    private String subjectSummary;
    private Integer subjectCount;
    private Integer enteredCount;
    private boolean closed;
    private String title;
    private String date;
    private String subject;
    private int maxScore;
    private Integer participantCount;
    private Integer absentCount;
    private Integer totalStudents;
    private Double average;
    private Double high;
    private Double low;
    private Double median;
    private List<ExamSectionResponse> sections;
    private List<ExamScoreResponse> scores;
    private LocalDateTime createdAt;
}
