package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.FeeUpdateRequest;
import com.hiacademy.api.dto.request.StudentRequest;
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

/**
 * @deprecated 학부모 테이블 통합 이전 호환 — {@link AdminStudentController} 사용
 */
@Deprecated
@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/parents")
public class AdminParentController {
    private final StudentAdminService studentSvc;

    public AdminParentController(StudentAdminService studentSvc) {
        this.studentSvc = studentSvc;
    }

    @GetMapping
    public ApiResponse<List<StudentResponse>> list(
            Authentication auth,
            @RequestParam(required = false) Integer yearMonth) {
        return ApiResponse.ok(studentSvc.listStudents(auth, yearMonth));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentResponse> create(
            Authentication auth,
            @Valid @RequestBody StudentRequest req) {
        return ApiResponse.ok(studentSvc.createStudent(auth, req));
    }

    @DeleteMapping("/students/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteStudent(Authentication auth, @PathVariable Long studentId) {
        studentSvc.deleteStudent(auth, studentId);
        return ApiResponse.noContent("삭제 완료");
    }

    @PatchMapping("/students/{studentId}/fees")




































































































































































































































































































































































































    public ApiResponse<FeeResponse> updateFee(
            Authentication auth,
            @PathVariable Long studentId,
            @RequestBody FeeUpdateRequest req) {
        return ApiResponse.ok(studentSvc.updateFee(auth, studentId, req));
    }
}
