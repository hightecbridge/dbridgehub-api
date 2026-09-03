package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.AdminConsultRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.ConsultResponse;
import com.hiacademy.api.dto.response.ConsultStatsResponse;
import com.hiacademy.api.dto.response.CounselorResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/consultations")
public class AdminConsultController {
    private final ConsultationService svc;

    public AdminConsultController(ConsultationService svc) {
        this.svc = svc;
    }

    @GetMapping
    public ApiResponse<List<ConsultResponse>> list(
            Authentication auth,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String kind) {
        return ApiResponse.ok(svc.listByAcademy(AuthHelper.academyId(auth), year, month, kind));
    }

    @GetMapping("/stats")
    public ApiResponse<ConsultStatsResponse> stats(
            Authentication auth,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        LocalDate now = LocalDate.now();
        int y = year != null ? year : now.getYear();
        int m = month != null ? month : now.getMonthValue();
        return ApiResponse.ok(svc.stats(AuthHelper.academyId(auth), y, m));
    }

    @GetMapping("/counselors")
    public ApiResponse<List<CounselorResponse>> counselors(Authentication auth) {
        return ApiResponse.ok(svc.counselors(AuthHelper.academyId(auth)));
    }

    @GetMapping("/students/{studentId}")
    public ApiResponse<List<ConsultResponse>> byStudent(Authentication auth, @PathVariable Long studentId) {
        return ApiResponse.ok(svc.listByStudent(auth, studentId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConsultResponse> create(Authentication auth, @Valid @RequestBody AdminConsultRequest req) {
        return ApiResponse.ok("상담이 등록되었습니다.", svc.create(auth, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<ConsultResponse> update(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody AdminConsultRequest req) {
        return ApiResponse.ok("상담이 수정되었습니다.", svc.update(auth, id, req));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ConsultResponse> updateStatus(
            Authentication auth,
            @PathVariable Long id,
            @RequestParam String status) {
        return ApiResponse.ok(svc.updateStatus(AuthHelper.academyId(auth), id, status));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(Authentication auth, @PathVariable Long id) {
        svc.delete(auth, id);
        return ApiResponse.noContent("삭제 완료");
    }
}
