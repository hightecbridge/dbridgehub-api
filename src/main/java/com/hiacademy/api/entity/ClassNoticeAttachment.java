package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_notice_attachments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassNoticeAttachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_notice_id", nullable = false)
    private ClassNotice classNotice;

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 120)
    private String contentType;

    private Long sizeBytes;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String data;

    @Column(nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
