package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_score_sections",
    uniqueConstraints = @UniqueConstraint(columnNames = {"exam_score_id", "exam_section_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamScoreSection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_score_id", nullable = false)
    private ExamScore examScore;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_section_id", nullable = false)
    private ExamSection section;

    /** 실제 점수. null = 미입력 */
    private Double score;

    /** 백분율 점수(0~100). null = 미입력 */
    private Double percent;
}
