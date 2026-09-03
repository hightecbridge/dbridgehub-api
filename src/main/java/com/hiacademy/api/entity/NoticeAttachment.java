package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notice_attachments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NoticeAttachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private NoticeItem notice;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 120)
    private String contentType;

    /** bytes */
    private Long sizeBytes;

    /** data URL or base64 payload */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String data;

    @Column(nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
