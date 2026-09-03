package com.hiacademy.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "students")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Student {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String grade;
    private LocalDate birthDate;
    /** 학생 본인 연락처 (선택) */
    private String phone;
    @Enumerated(EnumType.STRING) private StudentStatus status;

    /** 학부모(보호자) 이름 — 필수 */
    @Column(name = "parent_name") private String parentName;
    /** 학부모(보호자) 연락처 — 필수, SMS/로그인 기준 */
    @Column(name = "parent_phone") private String parentPhone;
    private String loginPhone;
    private String loginPassword;
    private String badgeColor;
    private String badgeTextColor;
    private boolean kakaoLinked;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "academy_id")
    private Academy academy;

    /** @deprecated parents 테이블 통합 이전 호환용 — 신규 등록 시 미사용 */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id")
    private Parent parent;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "classroom_id")
    private ClassRoom classroom;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default private List<FeeRecord> fees = new ArrayList<>();

    @CreationTimestamp private LocalDateTime createdAt;
    /** 가장 최근 퇴원 처리 시각 — 월별 퇴원 통계용. 재원 복귀 후에도 유지 */
    private LocalDateTime withdrawnAt;

    public Academy resolveAcademy() {
        if (academy != null) return academy;
        if (parent != null && parent.getAcademy() != null) return parent.getAcademy();
        return null;
    }

    public String resolveParentName() {
        if (parentName != null && !parentName.isBlank()) return parentName;
        if (parent != null) return parent.getName();
        return "";
    }

    public String resolveParentPhone() {
        if (parentPhone != null && !parentPhone.isBlank()) return parentPhone;
        if (parent != null) return parent.getPhone();
        return "";
    }

    public String resolveLoginPhone() {
        if (loginPhone != null && !loginPhone.isBlank()) return loginPhone;
        if (parent != null && parent.getLoginPhone() != null) return parent.getLoginPhone();
        return resolveParentPhone();
    }
}
