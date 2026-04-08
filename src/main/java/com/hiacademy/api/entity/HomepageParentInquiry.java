package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 랜딩 페이지 학부모(보호자) 문의 — 가입 문의와 별도 테이블. */
@Entity
@Table(name = "homepage_parent_inquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomepageParentInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(length = 100)
    private String childName;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 200)
    private String source;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
