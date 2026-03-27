package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "attend_sheets",
    uniqueConstraints = @UniqueConstraint(columnNames = {"classroom_id","attend_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendSheet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "classroom_id", nullable = false)
    private ClassRoom classroom;
    @Column(nullable = false) private LocalDate attendDate;
    @OneToMany(mappedBy = "sheet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<AttendRecord> records = new ArrayList<>();
    @CreationTimestamp private LocalDateTime createdAt;
}
