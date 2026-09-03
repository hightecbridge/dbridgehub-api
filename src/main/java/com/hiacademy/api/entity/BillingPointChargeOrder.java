package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 포인트 충전 주문(결제 요청 전 저장, 승인 후 DONE). */
@Entity
@Table(name = "billing_point_charge_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingPointChargeOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @Column(nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(nullable = false)
    private long amountKrw;

    /** READY | DONE | FAILED */
    @Column(nullable = false, length = 16)
    private String status;

    @Column(length = 200)
    private String paymentKey;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime confirmedAt;
}
