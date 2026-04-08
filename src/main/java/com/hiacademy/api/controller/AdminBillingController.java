package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.BillingSmsRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.BillingSummaryResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.AdminBillingService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/billing")
public class AdminBillingController {

    private final AdminBillingService svc;

    public AdminBillingController(AdminBillingService svc) {
        this.svc = svc;
    }

    @GetMapping
    public ApiResponse<BillingSummaryResponse> summary(Authentication auth) {
        return ApiResponse.ok(svc.getSummary(AuthHelper.academyId(auth)));
    }

    @PostMapping("/subscribe")
    public ApiResponse<BillingSummaryResponse> subscribe(Authentication auth) {
        return ApiResponse.ok("구독이 반영되었습니다.", svc.subscribe(AuthHelper.academyId(auth)));
    }

    @PostMapping("/sms")
    public ApiResponse<BillingSummaryResponse> sendSms(Authentication auth, @Valid @RequestBody BillingSmsRequest req) {
        return ApiResponse.ok("발송 처리되었습니다.", svc.sendSms(AuthHelper.academyId(auth), req));
    }
}
