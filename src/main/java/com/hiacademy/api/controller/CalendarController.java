package com.hiacademy.api.controller;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.dto.request.CalendarEventRequest;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.service.CalendarService;
import com.hiacademy.api.service.AdminAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping(ServicePaths.ACADEMY + "/admin/events")
public class CalendarController {
    private final CalendarService svc;
    private final AdminAccessService access;
    public CalendarController(CalendarService svc, AdminAccessService access) { this.svc=svc; this.access=access; }
    @GetMapping public ApiResponse<List<EventResponse>> list(Authentication auth) { return ApiResponse.ok(svc.list(AuthHelper.academyId(auth))); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EventResponse> create(Authentication auth, @Valid @RequestBody CalendarEventRequest req) {
        access.requireDirector(auth);
        return ApiResponse.ok(svc.create(AuthHelper.academyId(auth),req));
    }
    @PutMapping("/{id}") public ApiResponse<EventResponse> update(Authentication auth, @PathVariable Long id, @Valid @RequestBody CalendarEventRequest req) {
        access.requireDirector(auth);
        return ApiResponse.ok(svc.update(AuthHelper.academyId(auth),id,req));
    }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(Authentication auth, @PathVariable Long id) {
        access.requireDirector(auth);
        svc.delete(AuthHelper.academyId(auth),id);
        return ApiResponse.noContent("삭제 완료");
    }
}
