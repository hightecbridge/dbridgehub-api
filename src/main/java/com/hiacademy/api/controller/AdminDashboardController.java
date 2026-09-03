package com.hiacademy.api.controller;

import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.DashboardStatsResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/dashboard")
public class AdminDashboardController {
    private final DashboardService svc;

    public AdminDashboardController(DashboardService svc) {
        this.svc = svc;
    }

    @GetMapping("/stats")
    public ApiResponse<DashboardStatsResponse> stats(
            Authentication auth,
            @RequestParam(required = false) Integer year) {
        return ApiResponse.ok(svc.stats(auth, year));
    }
}
