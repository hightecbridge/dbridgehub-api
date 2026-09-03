package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_scores",
    uniqueConstraints = @UniqueConstraint(columnNames = {"exam_id", "student_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamScore {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** null = 미응시 */
    private Double score;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @OneToMany(mappedBy = "examScore", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExamScoreSection> sectionScores = new ArrayList<>();

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
