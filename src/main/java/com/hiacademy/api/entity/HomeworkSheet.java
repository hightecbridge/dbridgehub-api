package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "homework_sheets",
    uniqueConstraints = @UniqueConstraint(columnNames = {"classroom_id","homework_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HomeworkSheet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "classroom_id", nullable = false)
    private ClassRoom classroom;
    @Column(nullable = false) private LocalDate homeworkDate;
    @Column(nullable = false) private String    title;
    @OneToMany(mappedBy = "sheet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<HomeworkRecord> records = new ArrayList<>();
    @CreationTimestamp private LocalDateTime createdAt;
}
