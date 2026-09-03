package com.hiacademy.api.service;
import com.hiacademy.api.dto.request.ClassRoomRequest;
import com.hiacademy.api.dto.response.ClassRoomResponse;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service @Transactional
public class ClassRoomService {
    private final ClassRoomRepository clsRepo;
    private final AcademyRepository academyRepo;
    private final UserRepository userRepo;
    private final AdminAccessService access;

    public ClassRoomService(
            ClassRoomRepository c,
            AcademyRepository a,
            UserRepository userRepo,
            AdminAccessService access) {
        this.clsRepo = c;
        this.academyRepo = a;
        this.userRepo = userRepo;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<ClassRoomResponse> list(Authentication auth) {
        AdminAccessService.Scope scope = access.resolve(auth);
        return clsRepo.findAllByAcademyIdWithTeacher(scope.academyId()).stream()
            .filter(c -> scope.allowsClassroom(c.getId()))
            .map(Mapper::toClassRoom)
            .toList();
    }

    public ClassRoomResponse create(Authentication auth, ClassRoomRequest req) {
        AdminAccessService.Scope scope = access.resolve(auth);
        scope.requireDirector();
        Academy a = academyRepo.getReferenceById(scope.academyId());
        ClassRoom c = ClassRoom.builder()
            .name(req.getName()).subject(req.getSubject())
            .schedule(req.getSchedule()).capacity(req.getCapacity())
            .tuitionFee(req.getTuitionFee()).bookFee(req.getBookFee())
            .color(req.getColor()).textColor(req.getTextColor()).academy(a).build();
        applyTeacher(scope.academyId(), c, req);
        return Mapper.toClassRoom(clsRepo.save(c));
    }

    public ClassRoomResponse update(Authentication auth, Long id, ClassRoomRequest req) {
        AdminAccessService.Scope scope = access.resolve(auth);
        scope.requireDirector();
        ClassRoom c = clsRepo.findByIdAndAcademy_Id(id, scope.academyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.setName(req.getName());
        c.setSubject(req.getSubject());
        c.setSchedule(req.getSchedule());
        c.setCapacity(req.getCapacity());
        c.setTuitionFee(req.getTuitionFee());
        c.setBookFee(req.getBookFee());
        c.setColor(req.getColor());
        c.setTextColor(req.getTextColor());
        applyTeacher(scope.academyId(), c, req);
        return Mapper.toClassRoom(clsRepo.save(c));
    }

    public void delete(Authentication auth, Long id) {
        AdminAccessService.Scope scope = access.resolve(auth);
        scope.requireDirector();
        clsRepo.delete(clsRepo.findByIdAndAcademy_Id(id, scope.academyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    private void applyTeacher(Long academyId, ClassRoom c, ClassRoomRequest req) {
        if (req.getTeacherUserId() != null) {
            User teacher = userRepo.findByIdAndAcademy_Id(req.getTeacherUserId(), academyId)
                .filter(u -> u.getRole() == UserRole.TEACHER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "담당 교사를 찾을 수 없습니다."));
            c.setTeacherUser(teacher);
            c.setTeacher(teacher.getName());
            return;
        }
        c.setTeacherUser(null);
        String name = req.getTeacher() == null ? "" : req.getTeacher().trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "담당 교사를 입력하세요.");
        }
        c.setTeacher(name);
    }
}
