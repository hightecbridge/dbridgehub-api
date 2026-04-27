package com.sms.aligo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.hiacademy.api.dto.response.ApiResponse;
import com.sms.aligo.dto.AligoSmsSendRequest;
import com.sms.aligo.service.AligoSmsService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/sms/aligo/send")
public class AligoSmsController {
    private final AligoSmsService aligoSmsService;

    public AligoSmsController(AligoSmsService aligoSmsService) {
        this.aligoSmsService = aligoSmsService;
    }

    @PostMapping("/sms")
    public ApiResponse<JsonNode> sendSms(@Valid @ModelAttribute AligoSmsSendRequest request) {
        return ApiResponse.ok(aligoSmsService.sendSms(request));
    }

    @PostMapping("/lms")
    public ApiResponse<JsonNode> sendLms(@Valid @ModelAttribute AligoSmsSendRequest request) {
        return ApiResponse.ok(aligoSmsService.sendLms(request));
    }

    @PostMapping(value = "/mms", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<JsonNode> sendMms(
        @Valid @ModelAttribute AligoSmsSendRequest request,
        @ModelAttribute("image1") MultipartFile image1,
        @ModelAttribute("image2") MultipartFile image2,
        @ModelAttribute("image3") MultipartFile image3
    ) {
        return ApiResponse.ok(aligoSmsService.sendMms(request, Arrays.asList(image1, image2, image3)));
    }
}
