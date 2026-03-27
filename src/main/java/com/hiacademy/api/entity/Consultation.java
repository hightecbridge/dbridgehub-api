package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "consultations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Consultation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @Column(nullable = false) private LocalDate consultDate;
    private String consultTime;
    @Enumerated(EnumType.STRING) private ConsultStatus status;
    @Column(columnDefinition = "TEXT") private String content;
    @CreationTimestamp private LocalDateTime createdAt;
}
