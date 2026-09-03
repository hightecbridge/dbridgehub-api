package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.ExamRequest;
import com.hiacademy.api.dto.request.ExamScoresRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.ExamPageResponse;
import com.hiacademy.api.dto.response.ExamResponse;
import com.hiacademy.api.dto.response.ExamStatsResponse;
import com.hiacademy.api.entity.ExamKind;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.AdminAccessService;
import com.hiacademy.api.service.GradeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/exams/regular")
public class AdminRegularExamController {
    private final GradeService gradeSvc;
    private final AdminAccessService access;

    public AdminRegularExamController(GradeService gradeSvc, AdminAccessService access) {
        this.gradeSvc = gradeSvc;
        this.access = access;
    }

    @GetMapping
    public ApiResponse<ExamPageResponse> list(
        Authentication auth,
        @RequestParam(required = false) String grade,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String q,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "true") boolean dateDesc
    ) {
        return ApiResponse.ok(gradeSvc.listRegularExams(access.resolve(auth), grade, from, to, status, q, page, size, dateDesc));
    }

    @GetMapping("/{examId:\\d+}")
    public ApiResponse<ExamResponse> get(Authentication auth, @PathVariable Long examId) {
        return ApiResponse.ok(gradeSvc.getExam(access.resolve(auth), examId, ExamKind.ALL));
    }

    @GetMapping("/{examId:\\d+}/stats")
    public ApiResponse<ExamStatsResponse> stats(Authentication auth, @PathVariable Long examId) {
        gradeSvc.getExam(access.resolve(auth), examId, ExamKind.ALL);
        return ApiResponse.ok(gradeSvc.examStats(access.resolve(auth), examId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExamResponse> create(Authentication auth, @Valid @RequestBody ExamRequest req) {
        return ApiResponse.ok(gradeSvc.createExam(access.resolve(auth), req, ExamKind.ALL));
    }

    @PutMapping("/{examId:\\d+}")
    public ApiResponse<ExamResponse> update(Authentication auth, @PathVariable Long examId, @Valid @RequestBody ExamRequest req) {
        return ApiResponse.ok(gradeSvc.updateExam(access.resolve(auth), examId, req, ExamKind.ALL));
    }

    @DeleteMapping("/{examId:\\d+}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable Long examId) {
        gradeSvc.deleteExam(access.resolve(auth), examId, ExamKind.ALL);
        return ApiResponse.noContent("삭제 완료");
    }

    @PutMapping("/{examId:\\d+}/scores")
    public ApiResponse<ExamResponse> saveScores(
        Authentication auth,
        @PathVariable Long examId,
        @Valid @RequestBody ExamScoresRequest req
    ) {
        return ApiResponse.ok(gradeSvc.saveScores(access.resolve(auth), examId, req, ExamKind.ALL));
    }

    @PostMapping("/{examId:\\d+}/close")
    public ApiResponse<ExamResponse> close(Authentication auth, @PathVariable Long examId) {
        return ApiResponse.ok(gradeSvc.closeExam(access.resolve(auth), examId, ExamKind.ALL));
    }
}
