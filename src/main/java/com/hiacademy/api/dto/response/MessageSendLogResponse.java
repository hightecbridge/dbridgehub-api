package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MessageSendLogResponse {
    private Long id;
    private String kind;
    private String provider;
    private String targetLabel;
    private String title;
    private String bodyPreview;
    private int recipientCount;
    private String messageType;
    private Integer deductedPoints;
    private Integer remainingPoints;
    private LocalDateTime createdAt;
}
