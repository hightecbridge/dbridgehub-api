package com.hiacademy.api.controller;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.dto.request.*;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping(ServicePaths.ACADEMY + "/admin/auth")
public class AdminAuthController {
    private final AdminAuthService svc;
    public AdminAuthController(AdminAuthService svc) { this.svc=svc; }
    @PostMapping("/signup") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> signup(@Valid @RequestBody AdminSignupRequest req) { return ApiResponse.ok("회원가입 성공",svc.signup(req)); }
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AdminLoginRequest req) { return ApiResponse.ok(svc.login(req)); }
    @PutMapping("/profile")
    public ApiResponse<AuthResponse> updateProfile(Authentication auth, @RequestBody UpdateProfileRequest req) { return ApiResponse.ok("프로필 수정 완료",svc.updateProfile(AuthHelper.subjectId(auth),req)); }
    @PatchMapping("/password")
    public ApiResponse<Void> changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest req) { svc.changePassword(AuthHelper.subjectId(auth),req); return ApiResponse.noContent("비밀번호 변경 완료"); }
}
