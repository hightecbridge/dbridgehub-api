package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "class_notices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClassNotice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String title;
    @Column(nullable = false, columnDefinition = "TEXT") private String body;
    @ElementCollection
    @CollectionTable(name = "class_notice_targets", joinColumns = @JoinColumn(name = "class_notice_id"))
    @Column(name = "target")
    @Builder.Default private List<String> targets = new ArrayList<>();
    private String imageUrl;
    @Lob
    @Column(name = "image_data", columnDefinition = "TEXT")
    private String imageData;
    @Column(nullable = false) private String date;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;
    @OneToMany(mappedBy = "classNotice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @Builder.Default private List<ClassNoticeAttachment> attachments = new ArrayList<>();
    @CreationTimestamp private LocalDateTime createdAt;
}
