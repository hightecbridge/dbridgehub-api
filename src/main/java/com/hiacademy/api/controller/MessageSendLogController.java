package com.hiacademy.api.controller;

import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.dto.request.MessageSendLogRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.MessageSendLogResponse;
import com.hiacademy.api.service.MessageSendLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/message-sends")
public class MessageSendLogController {
    private final MessageSendLogService svc;

    public MessageSendLogController(MessageSendLogService svc) {
        this.svc = svc;
    }

    @GetMapping
    public ApiResponse<List<MessageSendLogResponse>> list(Authentication auth) {
        return ApiResponse.ok(svc.list(AuthHelper.academyId(auth)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MessageSendLogResponse> create(Authentication auth, @Valid @RequestBody MessageSendLogRequest req) {
        return ApiResponse.ok(svc.create(AuthHelper.academyId(auth), req));
    }
}
