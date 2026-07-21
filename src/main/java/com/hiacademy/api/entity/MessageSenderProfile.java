package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 메시지 발신번호 프로필.
 * <ul>
 *   <li>{@code adminUserId == null} — 공통(기본) 발신번호 (전체 어드민 공용)</li>
 *   <li>{@code adminUserId != null} — 해당 어드민 계정(users.id) 전용 발신번호</li>
 * </ul>
 * DB에서 직접 관리하며, 메시지 화면에서는 번호를 선택·수정하지 않습니다.
 */
@Entity
@Table(name = "message_sender_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageSenderProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** null이면 공통 발신번호. users.id 와 매칭 */
    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(nullable = false, length = 20)
    private String senderNumber;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(length = 200)
    private String memo;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
