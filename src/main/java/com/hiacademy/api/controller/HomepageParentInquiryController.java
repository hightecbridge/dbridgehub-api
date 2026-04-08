package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.HomepageParentInquiryRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.HomepageParentInquiryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ServicePaths.HOMEPAGE + "/parent-inquiries")
public class HomepageParentInquiryController {

    private final HomepageParentInquiryService svc;

    public HomepageParentInquiryController(HomepageParentInquiryService svc) {
        this.svc = svc;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> create(@Valid @RequestBody HomepageParentInquiryRequest req) {
        svc.create(req);
        return ApiResponse.noContent("문의가 접수되었습니다.");
    }
}
