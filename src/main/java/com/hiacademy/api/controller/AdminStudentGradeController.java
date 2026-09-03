package com.hiacademy.api.controller;

import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.GradeHistoryResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.AdminAccessService;
import com.hiacademy.api.service.GradeService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/students/{studentId}/grades")
public class AdminStudentGradeController {
    private final GradeService gradeSvc;
    private final AdminAccessService access;

    public AdminStudentGradeController(GradeService gradeSvc, AdminAccessService access) {
        this.gradeSvc = gradeSvc;
        this.access = access;
    }

    @GetMapping
    public ApiResponse<GradeHistoryResponse> history(Authentication auth, @PathVariable Long studentId) {
        return ApiResponse.ok(gradeSvc.studentHistory(access.resolve(auth), studentId));
    }
}
