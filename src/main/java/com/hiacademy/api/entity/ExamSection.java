package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_sections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamSection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private int maxScore = 100;

    /** 전체 점수에 반영할 배점. 0이면 만점과 동일하게 취급 */
    @Column(nullable = false)
    @Builder.Default
    private int weight = 0;

    @Column(nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
