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
    /** 학원별 메뉴·기능 사용 및 학부모 노출 설정(JSON) */
    @Column(columnDefinition = "TEXT") private String menuSettingsJson;
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
    /** 토스 정기결제 고객 식별키 */
    @Column(length = 120)
    private String tossCustomerKey;
    /** 토스 빌링키(자동청구 실행용) */
    @Column(length = 255)
    private String tossBillingKey;
    /** 최근 카드등록 인증키(감사/추적용) */
    @Column(length = 255)
    private String tossAuthKey;
    /** 정기결제 활성 여부 */
    private Boolean autoBillingEnabled;
    /** 최근 빌링키 발급 시각 */
    private LocalDateTime billingKeyIssuedAt;
    /** 정기결제 카드 번호 뒤 4자리 (마스킹 표시용) */
    @Column(length = 4)
    private String billingCardLast4;
    /** 정기결제 카드사명 (토스 cardCompany) */
    @Column(length = 32)
    private String billingCardCompany;
    /** 토스 카드 발급사 코드(issuerCode, 2자리) */
    @Column(length = 8)
    private String billingCardIssuerCode;
    /** 카드 유효기간 월 (MM) */
    @Column(length = 2)
    private String billingCardExpMonth;
    /** 카드 유효기간 연 (YY 또는 YYYY) */
    @Column(length = 4)
    private String billingCardExpYear;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp   private LocalDateTime updatedAt;

    // Lombok annotation processing 이슈 대비: 정기결제 키 필드는 명시 메서드 제공
    public String getTossCustomerKey() { return tossCustomerKey; }
    public void setTossCustomerKey(String tossCustomerKey) { this.tossCustomerKey = tossCustomerKey; }
    public String getTossBillingKey() { return tossBillingKey; }
    public void setTossBillingKey(String tossBillingKey) { this.tossBillingKey = tossBillingKey; }
    public String getTossAuthKey() { return tossAuthKey; }
    public void setTossAuthKey(String tossAuthKey) { this.tossAuthKey = tossAuthKey; }
    public Boolean getAutoBillingEnabled() { return autoBillingEnabled; }
    public void setAutoBillingEnabled(Boolean autoBillingEnabled) { this.autoBillingEnabled = autoBillingEnabled; }
    public LocalDateTime getBillingKeyIssuedAt() { return billingKeyIssuedAt; }
    public void setBillingKeyIssuedAt(LocalDateTime billingKeyIssuedAt) { this.billingKeyIssuedAt = billingKeyIssuedAt; }
}
