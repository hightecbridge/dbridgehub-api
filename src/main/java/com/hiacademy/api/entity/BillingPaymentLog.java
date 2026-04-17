package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 관리자 결제 이력(구독·포인트 충전). */
@Entity
@Table(name = "billing_payment_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingPaymentLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    /** SUBSCRIPTION | POINT_CHARGE */
    @Column(nullable = false, length = 32)
    private String paymentType;

    @Column(nullable = false)
    private long amountKrw;

    @Column(length = 64)
    private String orderId;

    /** 화면 표시용 한 줄 요약 */
    @Column(nullable = false, length = 500)
    private String summary;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
