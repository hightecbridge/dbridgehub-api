package com.hiacademy.api.controller;
import com.hiacademy.api.platform.ServicePaths;
import com.hiacademy.api.dto.request.ClassRoomRequest;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.service.ClassRoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping(ServicePaths.ACADEMY + "/admin/classrooms")
public class ClassRoomController {
    private final ClassRoomService svc;
    public ClassRoomController(ClassRoomService svc) { this.svc=svc; }
    @GetMapping public ApiResponse<List<ClassRoomResponse>> list(Authentication auth) { return ApiResponse.ok(svc.list(auth)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ClassRoomResponse> create(Authentication auth, @Valid @RequestBody ClassRoomRequest req) { return ApiResponse.ok(svc.create(auth,req)); }
    @PutMapping("/{id}") public ApiResponse<ClassRoomResponse> update(Authentication auth, @PathVariable Long id, @Valid @RequestBody ClassRoomRequest req) { return ApiResponse.ok(svc.update(auth,id,req)); }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(Authentication auth, @PathVariable Long id) { svc.delete(auth,id); return ApiResponse.noContent("삭제 완료"); }
}
