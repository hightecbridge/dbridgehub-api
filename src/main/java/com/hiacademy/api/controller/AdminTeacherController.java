package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.TeacherCreateRequest;
import com.hiacademy.api.dto.request.TeacherUpdateRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.TeacherLoginPreviewResponse;
import com.hiacademy.api.dto.response.TeacherResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.AdminAccessService;
import com.hiacademy.api.service.TeacherAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/teachers")
public class AdminTeacherController {
    private final TeacherAccountService svc;
    private final AdminAccessService access;

    public AdminTeacherController(TeacherAccountService svc, AdminAccessService access) {
        this.svc = svc;
        this.access = access;
    }

    @GetMapping("/login-preview")
    public ApiResponse<TeacherLoginPreviewResponse> preview(
            Authentication auth,
            @RequestParam(defaultValue = "") String handle) {
        access.requireDirector(auth);
        return ApiResponse.ok(svc.preview(AuthHelper.academyId(auth), handle));
    }

    @GetMapping
    public ApiResponse<List<TeacherResponse>> list(Authentication auth) {
        access.requireDirector(auth);
        return ApiResponse.ok(svc.list(AuthHelper.academyId(auth)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeacherResponse> create(Authentication auth, @Valid @RequestBody TeacherCreateRequest req) {
        access.requireDirector(auth);
        return ApiResponse.ok("교사 계정이 생성되었습니다.", svc.create(AuthHelper.academyId(auth), req));
    }

    @PutMapping("/{id}")
    public ApiResponse<TeacherResponse> update(
            Authentication auth,
            @PathVariable Long id,
            @RequestBody TeacherUpdateRequest req) {
        access.requireDirector(auth);
        return ApiResponse.ok("교사 정보가 저장되었습니다.", svc.update(AuthHelper.academyId(auth), id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable Long id) {
        access.requireDirector(auth);
        svc.delete(AuthHelper.academyId(auth), id);
        return ApiResponse.noContent("교사 계정이 삭제되었습니다.");
    }
}
