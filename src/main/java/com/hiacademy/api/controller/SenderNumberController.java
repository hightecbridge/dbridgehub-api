package com.hiacademy.api.controller;

import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.request.SenderNumberRequest;
import com.hiacademy.api.dto.response.SenderNumberResponse;
import com.hiacademy.api.service.SenderNumberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/sender-numbers")
public class SenderNumberController {
    private final SenderNumberService svc;

    public SenderNumberController(SenderNumberService svc) {
        this.svc = svc;
    }

    @GetMapping
    public ApiResponse<List<SenderNumberResponse>> list(Authentication auth) {
        return ApiResponse.ok(svc.list(AuthHelper.academyId(auth)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SenderNumberResponse> create(Authentication auth, @Valid @RequestBody SenderNumberRequest req) {
        return ApiResponse.ok(svc.create(AuthHelper.academyId(auth), req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable Long id) {
        svc.delete(AuthHelper.academyId(auth), id);
        return ApiResponse.noContent("삭제 완료");
    }

    @PatchMapping("/{id}/default")
    public ApiResponse<SenderNumberResponse> setDefault(Authentication auth, @PathVariable Long id) {
        return ApiResponse.ok(svc.setDefault(AuthHelper.academyId(auth), id));
    }
}

