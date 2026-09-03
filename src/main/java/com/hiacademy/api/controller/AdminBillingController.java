package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.BillingPointChargeRequest;
import com.hiacademy.api.dto.request.BillingSmsRequest;
import com.hiacademy.api.dto.request.BillingSubscribeRequest;
import com.hiacademy.api.dto.request.BillingAutoSubscribeRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.BillingPaymentResponse;
import com.hiacademy.api.dto.response.BillingPointChargePrepareResponse;
import com.hiacademy.api.dto.response.BillingSummaryResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.AdminBillingService;
import com.hiacademy.api.service.AdminAccessService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/billing")
public class AdminBillingController {

    private final AdminBillingService svc;
    private final AdminAccessService access;

    public AdminBillingController(AdminBillingService svc, AdminAccessService access) {
        this.svc = svc;
        this.access = access;
    }

    @GetMapping
    public ApiResponse<BillingSummaryResponse> summary(Authentication auth) {
        return ApiResponse.ok(svc.getSummary(AuthHelper.academyId(auth)));
    }

    @PostMapping("/subscribe")
    public ApiResponse<BillingSummaryResponse> subscribe(Authentication auth, @RequestBody(required = false) BillingSubscribeRequest req) {
        access.requireDirector(auth);
        return ApiResponse.ok("구독이 반영되었습니다.", svc.subscribe(AuthHelper.academyId(auth), req));
    }

    @PostMapping("/subscribe/auto-register")
    public ApiResponse<BillingSummaryResponse> registerAutoBilling(Authentication auth, @RequestBody BillingAutoSubscribeRequest req) {
        access.requireDirector(auth);
        return ApiResponse.ok("정기결제가 등록되었습니다.", svc.registerAndChargeAutoBilling(AuthHelper.academyId(auth), req));
    }

    @PostMapping({"/subscribe/upgrade", "/subscribe/change-plan"})
    public ApiResponse<BillingSummaryResponse> changePlan(Authentication auth, @RequestBody BillingSubscribeRequest req) {
        access.requireDirector(auth);
        return ApiResponse.ok("요금제가 변경되었습니다.", svc.changeBillingPlan(AuthHelper.academyId(auth), req));
    }

    @PostMapping("/billing-key/change")
    public ApiResponse<BillingSummaryResponse> changeBillingCard(Authentication auth, @RequestBody BillingAutoSubscribeRequest req) {
        access.requireDirector(auth);
        return ApiResponse.ok("카드 정보가 변경되었습니다.", svc.changeBillingCard(AuthHelper.academyId(auth), req));
    }

    @PostMapping("/sms")
    public ApiResponse<BillingSummaryResponse> sendSms(Authentication auth, @Valid @RequestBody BillingSmsRequest req) {
        access.requireDirector(auth);
        return ApiResponse.ok("발송 처리되었습니다.", svc.sendSms(AuthHelper.academyId(auth), req));
    }

    @PostMapping("/points/prepare")
    public ApiResponse<BillingPointChargePrepareResponse> preparePointCharge(Authentication auth, @RequestBody BillingPointChargeRequest req) {
        access.requireDirector(auth);
        return ApiResponse.ok(svc.preparePointCharge(AuthHelper.academyId(auth), req.getPoints()));
    }

    @PostMapping("/points/charge")
    public ApiResponse<BillingSummaryResponse> chargePoints(Authentication auth, @RequestBody BillingPointChargeRequest req) {
        access.requireDirector(auth);
        return ApiResponse.ok("포인트가 충전되었습니다.", svc.chargePoints(AuthHelper.academyId(auth), req));
    }

    @GetMapping("/payments")
    public ApiResponse<List<BillingPaymentResponse>> payments(Authentication auth) {
        access.requireDirector(auth);
        return ApiResponse.ok(svc.listPayments(AuthHelper.academyId(auth)));
    }
}
