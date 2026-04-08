package com.hiacademy.api.controller;

import com.hiacademy.api.dto.request.PushTokenRequest;
import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.service.ParentPushTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ServicePaths.ACADEMY + "/parent/push-token")
public class ParentPushController {

    private final ParentPushTokenService pushTokenSvc;

    public ParentPushController(ParentPushTokenService pushTokenSvc) {
        this.pushTokenSvc = pushTokenSvc;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(Authentication auth, @Valid @RequestBody PushTokenRequest req) {
        pushTokenSvc.register(AuthHelper.subjectId(auth), req.getExpoPushToken());
        return ApiResponse.ok("registered", null);
    }

    @DeleteMapping
    public ApiResponse<Void> unregister(Authentication auth) {
        pushTokenSvc.unregisterAll(AuthHelper.subjectId(auth));
        return ApiResponse.noContent("ok");
    }
}
