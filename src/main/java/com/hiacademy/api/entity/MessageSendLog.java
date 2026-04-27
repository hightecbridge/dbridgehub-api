package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_send_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageSendLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageSendKind kind;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) default 'ALIGO'")
    private MessageGatewayProvider provider = MessageGatewayProvider.ALIGO;

    /** 표시용: 반명·전체·결제 등 */
    @Column(nullable = false, length = 200)
    private String targetLabel;

    /** 목록 제목(메시지 첫 줄 또는 요약) */
    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String bodyPreview;

    @Column(nullable = false)
    private int recipientCount;

    /** 실제 발송 타입(SMS/LMS/MMS/KAKAO_ALIMTALK 등) */
    @Column(length = 30)
    private String messageType;

    /** 해당 발송에서 차감된 포인트 */
    private Integer deductedPoints;

    /** 차감 이후 잔여 포인트 */
    private Integer remainingPoints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
