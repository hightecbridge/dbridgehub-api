package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "attend_records",
    uniqueConstraints = @UniqueConstraint(columnNames = {"sheet_id","student_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sheet_id", nullable = false)
    private AttendSheet sheet;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AttendStatus status;
    private String note;
}
