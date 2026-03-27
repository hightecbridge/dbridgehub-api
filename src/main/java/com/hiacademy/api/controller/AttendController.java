package com.hiacademy.api.controller;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.dto.request.AttendSheetRequest;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.service.AttendService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping(ServicePaths.ACADEMY + "/admin/classrooms/{classroomId}/attend")
public class AttendController {
    private final AttendService svc;
    public AttendController(AttendService svc) { this.svc=svc; }
    @GetMapping public ApiResponse<List<AttendSheetResponse>> list(Authentication auth, @PathVariable Long classroomId) { return ApiResponse.ok(svc.listSheets(AuthHelper.academyId(auth),classroomId)); }
    @PostMapping public ApiResponse<AttendSheetResponse> save(Authentication auth, @PathVariable Long classroomId, @Valid @RequestBody AttendSheetRequest req) { return ApiResponse.ok(svc.saveSheet(AuthHelper.academyId(auth),classroomId,req)); }
    @DeleteMapping public ApiResponse<Void> delete(Authentication auth, @PathVariable Long classroomId, @RequestParam String date) {
        svc.deleteSheet(AuthHelper.academyId(auth), classroomId, LocalDate.parse(date));
        return ApiResponse.ok(null);
    }
}
