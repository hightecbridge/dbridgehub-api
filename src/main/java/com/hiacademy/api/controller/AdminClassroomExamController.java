package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.ExamRequest;
import com.hiacademy.api.dto.request.ExamScoresRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.ClassGradeStatsResponse;
import com.hiacademy.api.dto.response.ExamResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.AdminAccessService;
import com.hiacademy.api.service.GradeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 하위호환: 반 경로로 시험을 조회·저장하던 기존 클라이언트용 */
@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/classrooms/{classroomId}/exams")
public class AdminClassroomExamController {
    private final GradeService gradeSvc;
    private final AdminAccessService access;

    public AdminClassroomExamController(GradeService gradeSvc, AdminAccessService access) {
        this.gradeSvc = gradeSvc;
        this.access = access;
    }

    @GetMapping
    public ApiResponse<List<ExamResponse>> list(Authentication auth, @PathVariable Long classroomId) {
        return ApiResponse.ok(gradeSvc.listExams(access.resolve(auth), classroomId));
    }

    @GetMapping("/stats")
    public ApiResponse<ClassGradeStatsResponse> stats(Authentication auth, @PathVariable Long classroomId) {
        return ApiResponse.ok(gradeSvc.classStats(access.resolve(auth), classroomId));
    }

    @GetMapping("/{examId:\\d+}")
    public ApiResponse<ExamResponse> get(Authentication auth, @PathVariable Long classroomId, @PathVariable Long examId) {
        return ApiResponse.ok(gradeSvc.getExam(access.resolve(auth), classroomId, examId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExamResponse> create(Authentication auth, @PathVariable Long classroomId, @Valid @RequestBody ExamRequest req) {
        return ApiResponse.ok(gradeSvc.createExam(access.resolve(auth), classroomId, req));
    }

    @PutMapping("/{examId:\\d+}")
    public ApiResponse<ExamResponse> update(Authentication auth, @PathVariable Long classroomId, @PathVariable Long examId, @Valid @RequestBody ExamRequest req) {
        return ApiResponse.ok(gradeSvc.updateExam(access.resolve(auth), classroomId, examId, req));
    }

    @DeleteMapping("/{examId:\\d+}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable Long classroomId, @PathVariable Long examId) {
        gradeSvc.deleteExam(access.resolve(auth), classroomId, examId);
        return ApiResponse.noContent("삭제 완료");
    }

    @PutMapping("/{examId:\\d+}/scores")
    public ApiResponse<ExamResponse> saveScores(
        Authentication auth,
        @PathVariable Long classroomId,
        @PathVariable Long examId,
        @Valid @RequestBody ExamScoresRequest req
    ) {
        return ApiResponse.ok(gradeSvc.saveScores(access.resolve(auth), classroomId, examId, req));
    }
}
