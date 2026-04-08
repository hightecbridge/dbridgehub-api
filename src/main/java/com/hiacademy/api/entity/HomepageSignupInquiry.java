package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 랜딩 페이지 학원 도입·가입 문의 (학부모 문의와 분리). */
@Entity
@Table(name = "homepage_signup_inquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomepageSignupInquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String academyName;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(length = 80)
    private String studentCount;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 200)
    private String source;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
