package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity @Table(name = "academies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Academy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    private String address;
    @Column(columnDefinition = "TEXT") private String description;
    private String phone;
    @Column(columnDefinition = "TEXT") private String logoBase64;
    /** 臾대즺 泥댄뿕 醫낅즺 ?쒓컖 (媛????+30??. */
    private LocalDateTime trialEndsAt;
    /** ?좊즺 援щ룆 ?댁슜 留뚮즺 ?쒓컖(?붋룹뿰 寃곗젣 ???곗옣). */
    private LocalDateTime subscriptionEndsAt;
    /** 臾몄옄 諛쒖넚???ъ씤???쇰컲쨌寃곗젣 ?덈궡 ??李④컧). */
    private Integer smsPoints;
    /** TRIAL | ACTIVE | PAST_DUE */
    @Column(length = 20)
    private String billingStatus;
    /** standard | premium | enterprise ??援щ룆 ????? ?숈깮 ?깅줉 ?곹븳???ъ슜 */
    @Column(length = 32)
    private String billingPlanId;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp   private LocalDateTime updatedAt;
}
