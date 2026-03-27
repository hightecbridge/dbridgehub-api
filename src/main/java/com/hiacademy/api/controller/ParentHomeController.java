package com.hiacademy.api.controller;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.dto.request.ConsultRequest;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping(ServicePaths.ACADEMY + "/parent")
public class ParentHomeController {
    private final ParentAuthService   parentAuthSvc;
    private final AttendService       attendSvc;
    private final HomeworkService     hwSvc;
    private final ConsultationService consultSvc;
    public ParentHomeController(ParentAuthService p, AttendService a, HomeworkService h, ConsultationService c) {
        parentAuthSvc=p; attendSvc=a; hwSvc=h; consultSvc=c;
    }
    @GetMapping("/home") public ApiResponse<ParentHomeResponse> home(Authentication auth) { return ApiResponse.ok(parentAuthSvc.getHome(AuthHelper.subjectId(auth))); }
    @GetMapping("/students/{studentId}/attend") public ApiResponse<List<AttendRecordResponse>> attend(@PathVariable Long studentId) { return ApiResponse.ok(attendSvc.getStudentAttend(studentId)); }
    @GetMapping("/students/{studentId}/homework") public ApiResponse<List<HomeworkRecordResponse>> homework(@PathVariable Long studentId) { return ApiResponse.ok(hwSvc.getStudentHomework(studentId)); }
    @GetMapping("/consultations") public ApiResponse<List<ConsultResponse>> consultations(Authentication auth) { return ApiResponse.ok(consultSvc.listByParent(AuthHelper.subjectId(auth))); }
    @PostMapping("/consultations") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConsultResponse> requestConsult(Authentication auth, @Valid @RequestBody ConsultRequest req) { return ApiResponse.ok(consultSvc.request(AuthHelper.subjectId(auth),req)); }
}
