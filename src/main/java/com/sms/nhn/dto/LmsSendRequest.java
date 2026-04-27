package com.sms.nhn.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 장문 LMS 발송 요청 — NHN에서는 {@code POST /sender/mms} 로 장문(첨부 없음)을 발송합니다.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LmsSendRequest {

    private String templateId;
    @NotBlank
    private String title;
    @NotBlank
    private String body;
    @NotBlank
    private String sendNo;
    private String requestDate;
    private String senderGroupingKey;
    @NotEmpty
    @Valid
    private List<NhnRecipientDto> recipientList;
    private String userId;
    private String statsId;
    private String originCode;
}
