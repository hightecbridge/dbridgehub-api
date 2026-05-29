package com.hiacademy.api.controller;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.dto.request.*;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.service.ParentStudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping(ServicePaths.ACADEMY + "/admin/parents")
public class AdminParentController {
    private final ParentStudentService svc;
    public AdminParentController(ParentStudentService svc) { this.svc=svc; }
    @GetMapping public ApiResponse<List<ParentResponse>> list(
            Authentication auth,
            @RequestParam(required = false) Integer yearMonth) {
        return ApiResponse.ok(svc.listParents(AuthHelper.academyId(auth), yearMonth));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ParentResponse> create(Authentication auth, @Valid @RequestBody ParentRequest req) { return ApiResponse.ok(svc.createParent(AuthHelper.academyId(auth),req)); }
    @DeleteMapping("/{parentId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(Authentication auth, @PathVariable Long parentId) {
        svc.deleteParent(AuthHelper.academyId(auth), parentId);
        return ApiResponse.noContent("삭제 완료");
    }
    @PostMapping("/{parentId}/students") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudentResponse> addStudent(Authentication auth, @PathVariable Long parentId, @Valid @RequestBody StudentRequest req) { return ApiResponse.ok(svc.addStudent(AuthHelper.academyId(auth),parentId,req)); }
    @DeleteMapping("/students/{studentId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteStudent(Authentication auth, @PathVariable Long studentId) {
        svc.deleteStudent(AuthHelper.academyId(auth), studentId);
        return ApiResponse.noContent("삭제 완료");
    }
    @PatchMapping("/students/{studentId}/fees")
    public ApiResponse<FeeResponse> updateFee(Authentication auth, @PathVariable Long studentId, @RequestBody FeeUpdateRequest req) { return ApiResponse.ok(svc.updateFee(AuthHelper.academyId(auth),studentId,req)); }
}
