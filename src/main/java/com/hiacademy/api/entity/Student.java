package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "students")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String grade;
    private LocalDate birthDate;
    @Enumerated(EnumType.STRING) private StudentStatus status;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "classroom_id")
    private ClassRoom classroom;
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<FeeRecord> fees = new ArrayList<>();
}
