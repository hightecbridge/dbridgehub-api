package com.hiacademy.api.service;

import com.hiacademy.api.dto.request.TeacherCreateRequest;
import com.hiacademy.api.dto.request.TeacherUpdateRequest;
import com.hiacademy.api.dto.response.TeacherLoginPreviewResponse;
import com.hiacademy.api.dto.response.TeacherResponse;
import com.hiacademy.api.entity.ClassRoom;
import com.hiacademy.api.entity.User;
import com.hiacademy.api.entity.UserRole;
import com.hiacademy.api.repository.ClassRoomRepository;
import com.hiacademy.api.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TeacherAccountService {
    private final UserRepository userRepo;
    private final ClassRoomRepository clsRepo;
    private final PasswordEncoder encoder;

    public TeacherAccountService(UserRepository userRepo, ClassRoomRepository clsRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.clsRepo = clsRepo;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public TeacherLoginPreviewResponse preview(Long academyId, String handle) {
        User director = directorOf(academyId);
        String normalized = TeacherLoginIds.normalizeHandle(handle);
        if (!normalized.isBlank()) {
            TeacherLoginIds.validateHandle(normalized);
        }
        String suffix = TeacherLoginIds.suffixFromDirectorEmail(director.getEmail());
        return TeacherLoginPreviewResponse.builder()
            .handle(normalized)
            .suffix(suffix)
            .loginId(normalized.isBlank() ? suffix : TeacherLoginIds.loginId(normalized, director.getEmail()))
            .build();
    }

    @Transactional(readOnly = true)
    public List<TeacherResponse> list(Long academyId) {
        User director = directorOf(academyId);
        return userRepo.findAllByAcademy_IdAndRoleOrderByCreatedAtAsc(academyId, UserRole.TEACHER).stream()
            .map(u -> toResponse(u, director))
            .toList();
    }

    public TeacherResponse create(Long academyId, TeacherCreateRequest req) {
        User director = directorOf(academyId);
        String loginId = TeacherLoginIds.loginId(req.getHandle(), director.getEmail());
        if (userRepo.existsByEmailIgnoreCase(loginId) || userRepo.existsByEmail(loginId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 교사 아이디입니다.");
        }
        User teacher = userRepo.save(User.builder()
            .email(loginId)
            .password(encoder.encode(req.getPassword()))
            .name(req.getName().trim())
            .phone(blankToNull(req.getPhone()))
            .role(UserRole.TEACHER)
            .academy(director.getAcademy())
            .build());
        assignClassrooms(academyId, teacher, req.getClassroomIds());
        return toResponse(userRepo.findById(teacher.getId()).orElseThrow(), director);
    }

    public TeacherResponse update(Long academyId, Long teacherId, TeacherUpdateRequest req) {
        User director = directorOf(academyId);
        User teacher = loadTeacher(academyId, teacherId);
        if (req.getName() != null && !req.getName().isBlank()) {
            teacher.setName(req.getName().trim());
        }
        if (req.getPhone() != null) {
            teacher.setPhone(blankToNull(req.getPhone()));
        }
        if (req.getHandle() != null && !req.getHandle().isBlank()) {
            String loginId = TeacherLoginIds.loginId(req.getHandle(), director.getEmail());
            if (!loginId.equalsIgnoreCase(teacher.getEmail())
                && (userRepo.existsByEmailIgnoreCase(loginId) || userRepo.existsByEmail(loginId))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 교사 아이디입니다.");
            }
            teacher.setEmail(loginId);
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            if (req.getPassword().length() < 4) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 4자 이상이어야 합니다.");
            }
            teacher.setPassword(encoder.encode(req.getPassword()));
        }
        userRepo.save(teacher);
        if (req.getClassroomIds() != null) {
            assignClassrooms(academyId, teacher, req.getClassroomIds());
        } else if (req.getName() != null && !req.getName().isBlank()) {
            for (ClassRoom c : clsRepo.findAllByTeacherUser_IdAndAcademy_Id(teacher.getId(), academyId)) {
                c.setTeacher(teacher.getName());
            }
        }
        return toResponse(teacher, director);
    }

    public void delete(Long academyId, Long teacherId) {
        User teacher = loadTeacher(academyId, teacherId);
        for (ClassRoom c : clsRepo.findAllByTeacherUser_IdAndAcademy_Id(teacher.getId(), academyId)) {
            c.setTeacherUser(null);
        }
        userRepo.delete(teacher);
    }

    private void assignClassrooms(Long academyId, User teacher, List<Long> classroomIds) {
        Set<Long> wanted = classroomIds == null ? Set.of() : new HashSet<>(classroomIds);
        List<ClassRoom> all = clsRepo.findAllByAcademyIdWithTeacher(academyId);
        for (ClassRoom c : all) {
            if (wanted.contains(c.getId())) {
                c.setTeacherUser(teacher);
                c.setTeacher(teacher.getName());
            } else if (c.getTeacherUser() != null && c.getTeacherUser().getId().equals(teacher.getId())) {
                c.setTeacherUser(null);
            }
        }
        List<Long> missing = new ArrayList<>();
        for (Long id : wanted) {
            boolean found = all.stream().anyMatch(c -> c.getId().equals(id));
            if (!found) missing.add(id);
        }
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "담당 반을 찾을 수 없습니다.");
        }
    }

    private User loadTeacher(Long academyId, Long teacherId) {
        User teacher = userRepo.findByIdAndAcademy_Id(teacherId, academyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "교사를 찾을 수 없습니다."));
        if (teacher.getRole() != UserRole.TEACHER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "교사 계정만 관리할 수 있습니다.");
        }
        return teacher;
    }

    private User directorOf(Long academyId) {
        return userRepo.findFirstByAcademy_IdAndRoleOrderByIdAsc(academyId, UserRole.ADMIN)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "원장 계정을 찾을 수 없습니다."));
    }

    private TeacherResponse toResponse(User teacher, User director) {
        List<TeacherResponse.TeacherClassroomInfo> rooms = clsRepo
            .findAllByTeacherUser_IdAndAcademy_Id(teacher.getId(), director.getAcademy().getId())
            .stream()
            .map(c -> TeacherResponse.TeacherClassroomInfo.builder().id(c.getId()).name(c.getName()).build())
            .toList();
        return TeacherResponse.builder()
            .id(teacher.getId())
            .handle(TeacherLoginIds.handleFromLoginId(teacher.getEmail(), director.getEmail()))
            .loginId(teacher.getEmail())
            .name(teacher.getName())
            .phone(teacher.getPhone())
            .classrooms(rooms)
            .createdAt(teacher.getCreatedAt())
            .build();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
