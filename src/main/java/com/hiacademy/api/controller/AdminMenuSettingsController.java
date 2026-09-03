package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.UpdateMenuSettingsRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.MenuFeatureFlag;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.AdminAccessService;
import com.hiacademy.api.service.MenuSettingsService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/menu-settings")
public class AdminMenuSettingsController {
    private final MenuSettingsService svc;
    private final AdminAccessService access;

    public AdminMenuSettingsController(MenuSettingsService svc, AdminAccessService access) {
        this.svc = svc;
        this.access = access;
    }

    @GetMapping
    public ApiResponse<Map<String, MenuFeatureFlag>> get(Authentication auth) {
        return ApiResponse.ok(svc.get(AuthHelper.academyId(auth)));
    }

    @PutMapping
    public ApiResponse<Map<String, MenuFeatureFlag>> save(Authentication auth, @RequestBody UpdateMenuSettingsRequest req) {
        access.requireDirector(auth);
        return ApiResponse.ok("메뉴 설정이 저장되었습니다.", svc.save(AuthHelper.academyId(auth), req.getMenus()));
    }
}
