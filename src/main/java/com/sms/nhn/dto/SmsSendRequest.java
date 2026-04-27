package com.sms.nhn.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 단문 SMS 발송 요청 — NHN {@code POST /sender/sms} 본문과 동일 필드.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmsSendRequest {

    private String templateId;
    @NotBlank
    private String body;
    @NotBlank
    private String sendNo;
    /** 예약: {@code yyyy-MM-dd HH:mm} */
    private String requestDate;
    private String senderGroupingKey;
    @NotEmpty
    @Valid
    private List<NhnRecipientDto> recipientList;
    private String userId;
    private String statsId;
    private String originCode;
    private Boolean useConversion;
}
