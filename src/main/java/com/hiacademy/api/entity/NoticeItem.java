package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "notices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NoticeItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;
    @ElementCollection
    @CollectionTable(name = "notice_targets", joinColumns = @JoinColumn(name = "notice_id"))
    @Column(name = "target")
    @Builder.Default private List<String> targets = new ArrayList<>();
    // Legacy short URL column. Keep for compatibility.
    private String imageUrl;
    // Large payload (data URL/base64) column to avoid varchar length failures.
    @Lob
    @Column(name = "image_data", columnDefinition = "TEXT")
    private String imageData;
    @Column(nullable = false) private String date;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;
    @CreationTimestamp private LocalDateTime createdAt;
}
