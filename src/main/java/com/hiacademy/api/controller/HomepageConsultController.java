package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.HomepageConsultRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.HomepageConsultService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 디브릿지허브 홈페이지 "무료 상담 신청하기" 전용 엔드포인트.
 *
 * server.servlet.context-path 가 /api 인 경우,
 * 최종 경로는 POST /api/homepage/consults 가 된다.
 */
@RestController
@RequestMapping(ServicePaths.HOMEPAGE + "/consults")
public class HomepageConsultController {

    private final HomepageConsultService svc;

    public HomepageConsultController(HomepageConsultService svc) {
        this.svc = svc;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> create(@Valid @RequestBody HomepageConsultRequest req) {
        svc.create(req);
        return ApiResponse.noContent("무료 상담 신청이 접수되었습니다.");
    }
}

