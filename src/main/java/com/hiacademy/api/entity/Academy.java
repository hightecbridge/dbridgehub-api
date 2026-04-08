package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "academies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Academy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    private String address;
    @Column(columnDefinition = "TEXT") private String description;
    private String phone;
    @Column(columnDefinition = "TEXT") private String logoBase64;
    /** 무료 체험 종료 시각 (가입 시 +30일). */
    private LocalDateTime trialEndsAt;
    /** 문자 발송용 포인트(일반·결제 안내 등 차감). */
    private Integer smsPoints;
    /** TRIAL | ACTIVE | PAST_DUE */
    @Column(length = 20)
    private String billingStatus;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp   private LocalDateTime updatedAt;
}
