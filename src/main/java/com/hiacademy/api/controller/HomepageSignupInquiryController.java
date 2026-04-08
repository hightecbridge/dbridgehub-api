package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.HomepageSignupInquiryRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.HomepageSignupInquiryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ServicePaths.HOMEPAGE + "/signup-inquiries")
public class HomepageSignupInquiryController {

    private final HomepageSignupInquiryService svc;

    public HomepageSignupInquiryController(HomepageSignupInquiryService svc) {
        this.svc = svc;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> create(@Valid @RequestBody HomepageSignupInquiryRequest req) {
        svc.create(req);
        return ApiResponse.noContent("가입 문의가 접수되었습니다.");
    }
}
