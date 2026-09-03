package com.hiacademy.api.controller;

import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.dto.request.NoticeRequest;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.service.ClassNoticeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping(ServicePaths.ACADEMY + "/admin/class-notices")
public class ClassNoticeController {
    private final ClassNoticeService svc;
    public ClassNoticeController(ClassNoticeService svc) { this.svc = svc; }

    @GetMapping
    public ApiResponse<NoticePageResponse> list(
        Authentication auth,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String target,
        @RequestParam(required = false) String q
    ) { return ApiResponse.ok(svc.list(auth, page, size, target, q)); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<NoticeResponse> create(Authentication auth, @Valid @RequestBody NoticeRequest req) {
        return ApiResponse.ok(svc.create(auth, req));
    }

    @DeleteMapping("/{id}") public ApiResponse<Void> delete(Authentication auth, @PathVariable Long id) {
        svc.delete(auth, id);
        return ApiResponse.noContent("삭제 완료");
    }
}
