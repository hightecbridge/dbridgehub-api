package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "calendar_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CalendarEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String    title;
    @Column(nullable = false) private LocalDate date;
    private LocalDate endDate;
    @Enumerated(EnumType.STRING) private EventCategory category;
    @ElementCollection
    @CollectionTable(name = "event_targets", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "target")
    @Builder.Default private List<String> targets = new ArrayList<>();
    private String description;
    private String color;
    private boolean allDay;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academy_id", nullable = false)
    private Academy academy;
    @CreationTimestamp private LocalDateTime createdAt;
}
