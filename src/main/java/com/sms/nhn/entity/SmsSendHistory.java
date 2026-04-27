package com.sms.nhn.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sms_send_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsSendHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SmsMessageType messageType;

    @Column(nullable = false, length = 300)
    private String requestUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requestBody;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    @Column(length = 40)
    private String requestId;

    @Column(length = 10)
    private String statusCode;

    @Column
    private Integer resultCode;

    @Column(length = 500)
    private String resultMessage;

    @Column(nullable = false)
    private boolean successful;

    @Column(nullable = false)
    private int recipientCount;

    @OneToMany(mappedBy = "history", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SmsSendResult> results = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    public void addResult(SmsSendResult result) {
        result.setHistory(this);
        this.results.add(result);
    }
}
