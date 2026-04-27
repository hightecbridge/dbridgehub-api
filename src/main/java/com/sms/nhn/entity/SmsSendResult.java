package com.sms.nhn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sms_send_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsSendResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "history_id", nullable = false)
    private SmsSendHistory history;

    @Column(length = 30)
    private String recipientNo;

    @Column
    private Integer recipientSeq;

    @Column
    private Integer resultCode;

    @Column(length = 500)
    private String resultMessage;
}
