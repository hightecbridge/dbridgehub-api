package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "homework_records",
    uniqueConstraints = @UniqueConstraint(columnNames = {"sheet_id","student_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HomeworkRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sheet_id", nullable = false)
    private HomeworkSheet sheet;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @Column(nullable = false) private boolean done;
    @Column(columnDefinition = "TEXT") private String comment;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
