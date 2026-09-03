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
    private final GradeService        gradeSvc;
    private final ParentAccessService parentAccess;
    private final MenuSettingsService menuSettings;
    public ParentHomeController(
            ParentAuthService p, AttendService a, HomeworkService h, ConsultationService c,
            GradeService g, ParentAccessService pa, MenuSettingsService ms) {
        parentAuthSvc=p; attendSvc=a; hwSvc=h; consultSvc=c; gradeSvc=g; parentAccess=pa; menuSettings=ms;
    }
    @GetMapping("/home") public ApiResponse<ParentHomeResponse> home(Authentication auth) { return ApiResponse.ok(parentAuthSvc.getHome(AuthHelper.subjectId(auth))); }
    @GetMapping("/students/{studentId}/attend") public ApiResponse<List<AttendRecordResponse>> attend(Authentication auth, @PathVariable Long studentId) {
        menuSettings.requireParentFeature(AuthHelper.academyId(auth), "attend");
        parentAccess.requireChild(auth, studentId);
        return ApiResponse.ok(attendSvc.getStudentAttend(studentId));
    }
    @GetMapping("/students/{studentId}/homework") public ApiResponse<List<HomeworkRecordResponse>> homework(Authentication auth, @PathVariable Long studentId) {
        menuSettings.requireParentFeature(AuthHelper.academyId(auth), "homework");
        parentAccess.requireChild(auth, studentId);
        return ApiResponse.ok(hwSvc.getStudentHomework(studentId));
    }
    @GetMapping("/students/{studentId}/grades") public ApiResponse<GradeHistoryResponse> grades(Authentication auth, @PathVariable Long studentId) {
        Long academyId = AuthHelper.academyId(auth);
        menuSettings.requireAnyParentFeature(academyId, "examsRegular", "examsDaily");
        var child = parentAccess.requireChild(auth, studentId);
        boolean showRegular = menuSettings.parentVisible(academyId, "examsRegular");
        boolean showDaily = menuSettings.parentVisible(academyId, "examsDaily");
        return ApiResponse.ok(gradeSvc.parentHistory(child, showRegular, showDaily));
    }
    @GetMapping("/consultations") public ApiResponse<List<ConsultResponse>> consultations(Authentication auth) {
        menuSettings.requireParentFeature(AuthHelper.academyId(auth), "consult");
        return ApiResponse.ok(consultSvc.listByParent(AuthHelper.subjectId(auth)));
    }
    @PostMapping("/consultations") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConsultResponse> requestConsult(Authentication auth, @Valid @RequestBody ConsultRequest req) {
        menuSettings.requireParentFeature(AuthHelper.academyId(auth), "consult");
        return ApiResponse.ok(consultSvc.request(AuthHelper.subjectId(auth),req));
    }
}
