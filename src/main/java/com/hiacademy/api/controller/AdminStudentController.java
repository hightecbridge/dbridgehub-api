package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.FeeUpdateRequest;
import com.hiacademy.api.dto.request.StudentRequest;
import com.hiacademy.api.dto.request.StudentStatusRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.FeeResponse;
import com.hiacademy.api.dto.response.StudentResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.StudentAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/students")
public class AdminStudentController {
    private final StudentAdminService svc;

    public AdminStudentController(StudentAdminService svc) {
        this.svc = svc;
    }

    @GetMapping
    public ApiResponse<List<StudentResponse>> list(
            Authentication auth,
            @RequestParam(required = false) Integer yearMonth) {
        return ApiResponse.ok(svc.listStudents(auth, yearMonth));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentResponse> create(
            Authentication auth,
            @Valid @RequestBody StudentRequest req) {
        return ApiResponse.ok(svc.createStudent(auth, req));
    }

    @PatchMapping("/{studentId}/status")
    public ApiResponse<StudentResponse> updateStatus(
            Authentication auth,
            @PathVariable Long studentId,
            @Valid @RequestBody StudentStatusRequest req) {
        return ApiResponse.ok("재원 상태가 변경되었습니다.", svc.updateStatus(auth, studentId, req.getStatus()));
    }

    @DeleteMapping("/{studentId}")
    public ApiResponse<StudentResponse> delete(Authentication auth, @PathVariable Long studentId) {
        return ApiResponse.ok("퇴원 처리되었습니다.", svc.withdrawStudent(auth, studentId));
    }

    @PatchMapping("/{studentId}/fees")
    public ApiResponse<FeeResponse> updateFee(
            Authentication auth,
            @PathVariable Long studentId,
            @RequestBody FeeUpdateRequest req) {
        return ApiResponse.ok(svc.updateFee(auth, studentId, req));
    }
}
