package com.sms.nhn.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.hiacademy.api.dto.response.ApiResponse;
import com.sms.nhn.dto.LmsSendRequest;
import com.sms.nhn.dto.MmsSendRequest;
import com.sms.nhn.dto.SmsSendRequest;
import com.sms.nhn.service.NhnSmsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * NHN Cloud SMS v3.0 발송 프록시.
 * <p>
 * 전체 경로: {@code /api} (context-path) + {@code /sms/...}
 */
@RestController
@RequestMapping("/sms")
public class SmsController {

    private final NhnSmsService nhnSmsService;

    public SmsController(NhnSmsService nhnSmsService) {
        this.nhnSmsService = nhnSmsService;
    }

    /** 단문 SMS — {@code POST .../sender/sms} */
    @PostMapping("/send/sms")
    public ApiResponse<JsonNode> sendSms(@Valid @RequestBody SmsSendRequest request) {
        return ApiResponse.ok(nhnSmsService.sendSms(request));
    }

    /** 장문 LMS — {@code POST .../sender/mms} (첨부 없음) */
    @PostMapping("/send/lms")
    public ApiResponse<JsonNode> sendLms(@Valid @RequestBody LmsSendRequest request) {
        return ApiResponse.ok(nhnSmsService.sendLms(request));
    }

    /** 장문 MMS — {@code POST .../sender/mms} (선택적으로 {@code attachFileIdList}) */
    @PostMapping("/send/mms")
    public ApiResponse<JsonNode> sendMms(@Valid @RequestBody MmsSendRequest request) {
        return ApiResponse.ok(nhnSmsService.sendMms(request));
    }
}
