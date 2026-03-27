package com.hiacademy.api.controller;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.service.ConsultationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping(ServicePaths.ACADEMY + "/admin/consultations")
public class AdminConsultController {
    private final ConsultationService svc;
    public AdminConsultController(ConsultationService svc) { this.svc=svc; }
    @GetMapping public ApiResponse<List<ConsultResponse>> list(Authentication auth) { return ApiResponse.ok(svc.listByAcademy(AuthHelper.academyId(auth))); }
    @PatchMapping("/{id}/status") public ApiResponse<ConsultResponse> updateStatus(Authentication auth, @PathVariable Long id, @RequestParam String status) { return ApiResponse.ok(svc.updateStatus(AuthHelper.academyId(auth),id,status)); }
}
