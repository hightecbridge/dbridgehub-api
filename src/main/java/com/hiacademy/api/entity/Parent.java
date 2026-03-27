package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "parents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Parent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String phone;
    private String badgeColor;
    private String badgeTextColor;
    private boolean kakaoLinked;
    @Column(unique = true) private String loginPhone;
    private String loginPassword;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<Student> students = new ArrayList<>();
    @CreationTimestamp private LocalDateTime createdAt;
}
