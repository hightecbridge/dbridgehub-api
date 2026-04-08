package com.hiacademy.api.service;
import com.hiacademy.api.dto.request.*;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.*;
import com.hiacademy.api.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service @Transactional
public class AdminAuthService {
    private final UserRepository    userRepo;
    private final AcademyRepository academyRepo;
    private final PasswordEncoder   encoder;
    private final JwtUtil           jwt;
    public AdminAuthService(UserRepository u, AcademyRepository a, PasswordEncoder e, JwtUtil j) {
        this.userRepo=u; this.academyRepo=a; this.encoder=e; this.jwt=j;
    }
    public AuthResponse signup(AdminSignupRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"이미 사용 중인 이메일입니다.");
        Academy academy = academyRepo.save(Academy.builder()
            .name(req.getAcademyName()).address(req.getAcademyAddress())
            .description(req.getAcademyDesc())
            .phone(req.getPhone())
            .trialEndsAt(LocalDateTime.now().plusDays(30))
            .smsPoints(500)
            .billingStatus("TRIAL")
            .build());
        User user = userRepo.save(User.builder()
            .email(req.getEmail()).password(encoder.encode(req.getPassword()))
            .name(req.getName()).phone(req.getPhone())
            .role(UserRole.ADMIN).academy(academy).build());
        return build(user, academy);
    }
    public AuthResponse login(AdminLoginRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
            .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!encoder.matches(req.getPassword(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"이메일 또는 비밀번호가 올바르지 않습니다.");
        return build(user, user.getAcademy());
    }
    public AuthResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepo.findById(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (req.getName()!=null)               user.setName(req.getName());
        if (req.getPhone()!=null)              user.setPhone(req.getPhone());
        if (req.getProfileImageBase64()!=null) user.setProfileImageBase64(req.getProfileImageBase64());
        Academy a = user.getAcademy();
        if (a!=null) {
            if (req.getAcademyName()!=null)       a.setName(req.getAcademyName());
            if (req.getAcademyAddress()!=null)     a.setAddress(req.getAcademyAddress());
            if (req.getAcademyDesc()!=null)        a.setDescription(req.getAcademyDesc());
            if (req.getAcademyLogoBase64()!=null)  a.setLogoBase64(req.getAcademyLogoBase64());
            academyRepo.save(a);
        }
        return build(userRepo.save(user), a);
    }
    public AuthResponse getMe(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        return build(user, user.getAcademy());
    }

    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = userRepo.findById(userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!encoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"현재 비밀번호가 올바르지 않습니다.");
        user.setPassword(encoder.encode(req.getNewPassword()));
        userRepo.save(user);
    }
    private AuthResponse build(User user, Academy academy) {
        return AuthResponse.builder().token(jwt.generateAdminToken(user))
            .type("admin").id(user.getId()).name(user.getName()).email(user.getEmail())
            .phone(user.getPhone())
            .profileImageBase64(user.getProfileImageBase64())
            .createdAt(user.getCreatedAt())
            .role(user.getRole().name()).academy(Mapper.toAcademyInfo(academy)).build();
    }
}
