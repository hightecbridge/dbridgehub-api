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

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academy_id")
    private Academy academy;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "teacher_user_id")
    private User teacher;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConsultKind kind;

    @Column(nullable = false) private LocalDate consultDate;
    private String consultTime;
    @Enumerated(EnumType.STRING) private ConsultStatus status;
    @Column(columnDefinition = "TEXT") private String content;

    /** 신규 상담 대상 (재원생이 아닐 때) */
    private String prospectName;
    private String prospectPhone;
    private String prospectGrade;
    private String prospectParentName;

    @CreationTimestamp private LocalDateTime createdAt;

    public Academy resolveAcademy() {
        if (academy != null) return academy;
        if (student != null) return student.resolveAcademy();
        return null;
    }

    public String displayName() {
        if (student != null && student.getName() != null && !student.getName().isBlank()) {
            return student.getName();
        }
        if (prospectName != null && !prospectName.isBlank()) return prospectName;
        return "신규 상담";
    }
}
