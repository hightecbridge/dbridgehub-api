package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.ExamRequest;
import com.hiacademy.api.dto.request.ExamScoresRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.ExamResponse;
import com.hiacademy.api.dto.response.ExamStatsResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.AdminAccessService;
import com.hiacademy.api.service.GradeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/exams")
public class AdminExamController {
    private final GradeService gradeSvc;
    private final AdminAccessService access;

    public AdminExamController(GradeService gradeSvc, AdminAccessService access) {
        this.gradeSvc = gradeSvc;
        this.access = access;
    }

    @GetMapping
    public ApiResponse<List<ExamResponse>> list(Authentication auth) {
        return ApiResponse.ok(gradeSvc.listExams(access.resolve(auth)));
    }

    @GetMapping("/{examId:\\d+}")
    public ApiResponse<ExamResponse> get(Authentication auth, @PathVariable Long examId) {
        return ApiResponse.ok(gradeSvc.getExam(access.resolve(auth), examId));
    }

    @GetMapping("/{examId:\\d+}/stats")
    public ApiResponse<ExamStatsResponse> stats(Authentication auth, @PathVariable Long examId) {
        return ApiResponse.ok(gradeSvc.examStats(access.resolve(auth), examId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExamResponse> create(Authentication auth, @Valid @RequestBody ExamRequest req) {
        return ApiResponse.ok(gradeSvc.createExam(access.resolve(auth), req));
    }

    @PutMapping("/{examId:\\d+}")
    public ApiResponse<ExamResponse> update(Authentication auth, @PathVariable Long examId, @Valid @RequestBody ExamRequest req) {
        return ApiResponse.ok(gradeSvc.updateExam(access.resolve(auth), examId, req));
    }

    @DeleteMapping("/{examId:\\d+}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable Long examId) {
        gradeSvc.deleteExam(access.resolve(auth), examId);
        return ApiResponse.noContent("삭제 완료");
    }

    @PutMapping("/{examId:\\d+}/scores")
    public ApiResponse<ExamResponse> saveScores(
        Authentication auth,
        @PathVariable Long examId,
        @Valid @RequestBody ExamScoresRequest req
    ) {
        return ApiResponse.ok(gradeSvc.saveScores(access.resolve(auth), examId, req));
    }
}
