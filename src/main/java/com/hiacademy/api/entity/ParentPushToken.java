package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "parent_push_tokens", uniqueConstraints = @UniqueConstraint(name = "uk_parent_push_token", columnNames = "expo_push_token"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParentPushToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    /** DB 스키마 상 NOT NULL — 학부모와 동일 학원으로 유지 */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @Column(name = "expo_push_token", nullable = false, length = 512)
    private String expoPushToken;

    // Legacy schema compatibility: some DBs still enforce NOT NULL on `token`.
    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
