package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exams")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Exam {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String title;
    @Column(nullable = false) private LocalDate examDate;
    private String subject;

    @Column(nullable = false)
    @Builder.Default
    private int maxScore = 100;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private ExamKind kind = ExamKind.CLASS;

    /** 전체시험 대상 학년. null/blank = 전학년 */
    @Column(length = 40)
    private String targetGrade;

    @Column(nullable = false)
    @Builder.Default
    private boolean closed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id")
    private ClassRoom classroom;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "exam_classrooms",
        joinColumns = @JoinColumn(name = "exam_id"),
        inverseJoinColumns = @JoinColumn(name = "classroom_id")
    )
    @OrderBy("name ASC")
    @Builder.Default
    private List<ClassRoom> classrooms = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamScore> scores = new ArrayList<>();

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @Builder.Default
    private List<ExamSection> sections = new ArrayList<>();

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamClassroomInput> inputStatuses = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
}
