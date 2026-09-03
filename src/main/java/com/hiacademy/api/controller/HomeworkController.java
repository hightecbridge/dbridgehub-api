package com.hiacademy.api.controller;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.dto.request.*;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.service.AdminAccessService;
import com.hiacademy.api.service.HomeworkService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping(ServicePaths.ACADEMY + "/admin/classrooms/{classroomId}/homework")
public class HomeworkController {
    private final HomeworkService svc;
    private final AdminAccessService access;
    public HomeworkController(HomeworkService svc, AdminAccessService access) {
        this.svc = svc;
        this.access = access;
    }
    @GetMapping public ApiResponse<List<HomeworkSheetResponse>> list(Authentication auth, @PathVariable Long classroomId) {
        access.resolve(auth).requireClassroom(classroomId);
        return ApiResponse.ok(svc.listSheets(AuthHelper.academyId(auth),classroomId));
    }
    @PostMapping public ApiResponse<HomeworkSheetResponse> save(Authentication auth, @PathVariable Long classroomId, @Valid @RequestBody HomeworkSheetRequest req) {
        access.resolve(auth).requireClassroom(classroomId);
        return ApiResponse.ok(svc.saveSheet(AuthHelper.academyId(auth),classroomId,req));
    }
    @PatchMapping("/{sheetId}/students/{studentId}") public ApiResponse<HomeworkRecordResponse> updateRecord(
            Authentication auth, @PathVariable Long classroomId, @PathVariable Long sheetId, @PathVariable Long studentId, @RequestBody HomeworkRecordUpdate req) {
        access.resolve(auth).requireClassroom(classroomId);
        return ApiResponse.ok(svc.updateRecord(sheetId,studentId,req));
    }
    @DeleteMapping("/{sheetId}") public ApiResponse<Void> delete(Authentication auth, @PathVariable Long classroomId, @PathVariable Long sheetId) {
        access.resolve(auth).requireClassroom(classroomId);
        svc.deleteSheet(AuthHelper.academyId(auth), classroomId, sheetId);
        return ApiResponse.ok(null);
    }
}
