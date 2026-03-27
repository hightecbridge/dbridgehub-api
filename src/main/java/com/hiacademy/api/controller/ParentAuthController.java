package com.hiacademy.api.controller;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.dto.request.ParentLoginRequest;
import com.hiacademy.api.dto.request.ParentSignupRequest;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.service.ParentAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping(ServicePaths.ACADEMY + "/parent/auth")
public class ParentAuthController {
    private final ParentAuthService svc;
    public ParentAuthController(ParentAuthService svc) { this.svc=svc; }
    @PostMapping("/login") public ApiResponse<AuthResponse> login(@Valid @RequestBody ParentLoginRequest req) { return ApiResponse.ok(svc.login(req)); }
    @PostMapping("/signup") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResponse<AuthResponse> signup(@Valid @RequestBody ParentSignupRequest req) { return ApiResponse.ok(svc.signup(req)); }
}
