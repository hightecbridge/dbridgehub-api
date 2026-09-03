package com.hiacademy.api.controller;

import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.dto.request.MessageSendLogRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.MessageSendLogResponse;
import com.hiacademy.api.dto.response.MessageSenderInfoResponse;
import com.hiacademy.api.service.MessageSendLogService;
import com.hiacademy.api.service.MessageSenderResolverService;
import com.hiacademy.api.service.AdminAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/admin/message-sends")
public class MessageSendLogController {
    private final MessageSendLogService svc;
    private final MessageSenderResolverService senderResolver;
    private final AdminAccessService access;

    public MessageSendLogController(MessageSendLogService svc, MessageSenderResolverService senderResolver, AdminAccessService access) {
        this.svc = svc;
        this.senderResolver = senderResolver;
        this.access = access;
    }

    @GetMapping("/sender")
    public ApiResponse<MessageSenderInfoResponse> sender(Authentication auth) {
        var info = senderResolver.resolveInfoForUser(AuthHelper.subjectId(auth));
        return ApiResponse.ok(senderResolver.toResponse(info));
    }

    @GetMapping
    public ApiResponse<List<MessageSendLogResponse>> list(Authentication auth) {
        return ApiResponse.ok(svc.list(AuthHelper.academyId(auth)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MessageSendLogResponse> create(Authentication auth, @Valid @RequestBody MessageSendLogRequest req) {
        access.requireDirector(auth);
        return ApiResponse.ok(svc.create(AuthHelper.academyId(auth), AuthHelper.subjectId(auth), req));
    }
}
