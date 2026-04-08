package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 디브릿지허브 홈페이지의 "무료 상담 신청하기" 데이터를 저장하는 전용 테이블.
 * 학원/학부모 도메인과는 완전히 분리된다.
 */
@Entity
@Table(name = "homepage_consults")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomepageConsult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신청자 이름 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 연락처 (전화번호 등) */
    @Column(nullable = false, length = 50)
    private String phone;

    /** 상담을 희망하는 내용 / 메모 */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** 유입 경로나 페이지 정보 (선택) */
    @Column(length = 200)
    private String source;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

