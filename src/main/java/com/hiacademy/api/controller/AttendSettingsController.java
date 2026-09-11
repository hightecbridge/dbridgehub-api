package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.AttendSettingsRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.AttendSettingsResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.AdminAccessService;
import com.hiacademy.api.service.AttendHolidayService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/attend-settings")
public class AttendSettingsController {
    private final AttendHolidayService svc;
    private final AdminAccessService access;

    public AttendSettingsController(AttendHolidayService svc, AdminAccessService access) {
        this.svc = svc;
        this.access = access;
    }

    @GetMapping
    public ApiResponse<AttendSettingsResponse> get(Authentication auth) {
        access.resolve(auth);
        return ApiResponse.ok(svc.get(AuthHelper.academyId(auth)));
    }

    @PutMapping
    public ApiResponse<AttendSettingsResponse> save(Authentication auth, @RequestBody AttendSettingsRequest req) {
        access.resolve(auth);
        return ApiResponse.ok("출석 설정이 저장되었습니다.",
            svc.save(AuthHelper.academyId(auth), req.getExcludeHolidaysFromAttend()));
    }
}
