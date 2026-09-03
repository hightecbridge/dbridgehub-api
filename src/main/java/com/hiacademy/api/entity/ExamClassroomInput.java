package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_classroom_inputs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"exam_id", "classroom_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamClassroomInput {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classroom_id", nullable = false)
    private ClassRoom classroom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private ExamInputStatus status = ExamInputStatus.DRAFT;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
