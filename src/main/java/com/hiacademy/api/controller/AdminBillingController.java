package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.BillingPointChargeRequest;
import com.hiacademy.api.dto.request.BillingSmsRequest;
import com.hiacademy.api.dto.request.BillingSubscribeRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.BillingPaymentResponse;
import com.hiacademy.api.dto.response.BillingSummaryResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.AdminBillingService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ApiResponse<BillingSummaryResponse> subscribe(Authentication auth, @RequestBody(required = false) BillingSubscribeRequest req) {
        return ApiResponse.ok("구독이 반영되었습니다.", svc.subscribe(AuthHelper.academyId(auth), req));
    }

    @PostMapping("/sms")
    public ApiResponse<BillingSummaryResponse> sendSms(Authentication auth, @Valid @RequestBody BillingSmsRequest req) {
        return ApiResponse.ok("발송 처리되었습니다.", svc.sendSms(AuthHelper.academyId(auth), req));
    }

    @PostMapping("/points/charge")
    public ApiResponse<BillingSummaryResponse> chargePoints(Authentication auth, @RequestBody BillingPointChargeRequest req) {
        return ApiResponse.ok("포인트가 충전되었습니다.", svc.chargePoints(AuthHelper.academyId(auth), req));
    }

    @GetMapping("/payments")
    public ApiResponse<List<BillingPaymentResponse>> payments(Authentication auth) {
        return ApiResponse.ok(svc.listPayments(AuthHelper.academyId(auth)));
    }
}
