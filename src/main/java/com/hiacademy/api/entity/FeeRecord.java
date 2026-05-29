package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name = "fee_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeeRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String  label;
    @Column(nullable = false) private int     amount;
    @Column(nullable = false) private boolean paid;
    /** 완납 시 납부일 */
    private LocalDate paidAt;
    /** 완납 시 납부방법 (현금·카드·계좌이체·제로페이·기타 또는 기타 직접입력) */
    @Column(length = 64)
    private String paymentMethod;
    @Column(nullable = false) private int     yearMonth;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
