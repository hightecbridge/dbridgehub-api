package com.hiacademy.api.service;

import com.hiacademy.api.dto.request.AdminConsultRequest;
import com.hiacademy.api.dto.request.ConsultRequest;
import com.hiacademy.api.dto.response.ConsultResponse;
import com.hiacademy.api.dto.response.ConsultStatsResponse;
import com.hiacademy.api.dto.response.CounselorResponse;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ConsultationService {
    private final ConsultationRepository consultRepo;
    private final StudentRepository stuRepo;
    private final UserRepository userRepo;
    private final AcademyRepository academyRepo;
    private final AdminAccessService access;

    public ConsultationService(
            ConsultationRepository c,
            StudentRepository s,
            UserRepository u,
            AcademyRepository a,
            AdminAccessService access) {
        consultRepo = c;
        stuRepo = s;
        userRepo = u;
        academyRepo = a;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<ConsultResponse> listByAcademy(Long academyId, Integer year, Integer month, String kind) {
        List<Consultation> rows = (year != null && month != null)
            ? consultRepo.findAllByAcademyIdAndDateBetween(academyId, monthStart(year, month), monthEnd(year, month))
            : consultRepo.findAllByAcademyId(academyId);
        ConsultKind filterKind = parseKindOrNull(kind);
        return rows.stream()
            .filter(c -> filterKind == null || kindOf(c) == filterKind)
            .map(Mapper::toConsult)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsultResponse> listByStudent(Authentication auth, Long studentId) {
        AdminAccessService.Scope scope = access.resolve(auth);
        Student stu = stuRepo.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다."));
        if (!StudentAdminService.belongsToAcademy(stu, scope.academyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        scope.requireStudent(stu);
        return consultRepo.findAllByStudent_IdOrderByConsultDateDescIdDesc(studentId).stream()
            .map(Mapper::toConsult)
            .toList();
    }

    @Transactional(readOnly = true)
    public ConsultStatsResponse stats(Long academyId, int year, int month) {
        List<Consultation> rows = consultRepo.findAllByAcademyIdAndDateBetween(
            academyId, monthStart(year, month), monthEnd(year, month));
        int enrolled = 0;
        int prospect = 0;
        Map<Long, ConsultStatsResponse.CountItem> teachers = new LinkedHashMap<>();
        Map<String, ConsultStatsResponse.CountItem> students = new LinkedHashMap<>();
        Map<String, List<String>> byDate = new LinkedHashMap<>();

        for (Consultation c : rows) {
            if (kindOf(c) == ConsultKind.신규) prospect++;
            else enrolled++;

            User t = c.getTeacher();
            Long teacherId = t != null ? t.getId() : null;
            String teacherName = t != null ? t.getName() : "미지정";
            teachers.merge(teacherId == null ? -1L : teacherId,
                ConsultStatsResponse.CountItem.builder().id(teacherId).name(teacherName).count(1).build(),
                (a, b) -> {
                    a.setCount(a.getCount() + 1);
                    return a;
                });

            Long sid = c.getStudent() != null ? c.getStudent().getId() : null;
            String studentKey = sid != null ? "s:" + sid : "p:" + c.displayName();
            students.merge(studentKey,
                ConsultStatsResponse.CountItem.builder().id(sid).name(c.displayName()).count(1).build(),
                (a, b) -> {
                    a.setCount(a.getCount() + 1);
                    return a;
                });

            String date = c.getConsultDate().toString();
            byDate.computeIfAbsent(date, k -> new ArrayList<>()).add(c.displayName());
        }

        List<ConsultStatsResponse.DayItem> days = byDate.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> ConsultStatsResponse.DayItem.builder()
                .date(e.getKey())
                .count(e.getValue().size())
                .names(e.getValue())
                .build())
            .toList();

        List<ConsultStatsResponse.CountItem> teacherList = teachers.values().stream()
            .sorted(Comparator.comparingInt(ConsultStatsResponse.CountItem::getCount).reversed())
            .toList();
        List<ConsultStatsResponse.CountItem> studentList = students.values().stream()
            .sorted(Comparator.comparingInt(ConsultStatsResponse.CountItem::getCount).reversed())
            .toList();

        return ConsultStatsResponse.builder()
            .year(year).month(month)
            .totalCount(rows.size())
            .enrolledCount(enrolled)
            .prospectCount(prospect)
            .byTeacher(teacherList)
            .byStudent(studentList)
            .byDate(days)
            .build();
    }

    @Transactional(readOnly = true)
    public List<CounselorResponse> counselors(Long academyId) {
        return userRepo.findAllByAcademy_IdOrderByCreatedAtAsc(academyId).stream()
            .filter(u -> u.getRole() == UserRole.ADMIN || u.getRole() == UserRole.TEACHER)
            .sorted(Comparator.comparing((User u) -> u.getRole() == UserRole.ADMIN ? 0 : 1)
                .thenComparing(User::getName, Comparator.nullsLast(String::compareTo)))
            .map(u -> CounselorResponse.builder()
                .id(u.getId())
                .name(u.getName())
                .role(u.getRole() != null ? u.getRole().name() : null)
                .build())
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsultResponse> listByParent(Long studentId) {
        Student anchor = stuRepo.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Academy academy = anchor.resolveAcademy();
        if (academy == null) return List.of();
        return consultRepo.findAllByAcademyIdAndLoginPhone(academy.getId(), anchor.resolveLoginPhone())
            .stream().map(Mapper::toConsult).toList();
    }

    public ConsultResponse request(Long studentId, ConsultRequest req) {
        Student anchor = stuRepo.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Student stu = stuRepo.findById(req.getStudentId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!sameGuardian(anchor, stu)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return Mapper.toConsult(consultRepo.save(Consultation.builder()
            .student(stu)
            .academy(stu.resolveAcademy())
            .kind(ConsultKind.재원생)
            .consultDate(LocalDate.parse(req.getDate()))
            .consultTime(req.getTime())
            .content(req.getContent())
            .status(ConsultStatus.대기)
            .build()));
    }

    public ConsultResponse create(Authentication auth, AdminConsultRequest req) {
        AdminAccessService.Scope scope = access.resolve(auth);
        Consultation c = new Consultation();
        applyAdminRequest(scope, c, req, true);
        return Mapper.toConsult(consultRepo.save(c));
    }

    public ConsultResponse update(Authentication auth, Long id, AdminConsultRequest req) {
        AdminAccessService.Scope scope = access.resolve(auth);
        Consultation c = loadOwned(scope.academyId(), id);
        applyAdminRequest(scope, c, req, false);
        return Mapper.toConsult(consultRepo.save(c));
    }

    public ConsultResponse updateStatus(Long academyId, Long id, String status) {
        Consultation c = loadOwned(academyId, id);
        c.setStatus(parseStatus(status));
        return Mapper.toConsult(consultRepo.save(c));
    }

    public void delete(Authentication auth, Long id) {
        AdminAccessService.Scope scope = access.resolve(auth);
        Consultation c = loadOwned(scope.academyId(), id);
        consultRepo.delete(c);
    }

    private void applyAdminRequest(AdminAccessService.Scope scope, Consultation c, AdminConsultRequest req, boolean creating) {
        ConsultKind kind = parseKind(req.getKind());
        c.setKind(kind);
        c.setConsultDate(LocalDate.parse(req.getDate()));
        c.setConsultTime(blankToNull(req.getTime()));
        c.setContent(req.getContent());
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            c.setStatus(parseStatus(req.getStatus()));
        } else if (creating) {
            c.setStatus(ConsultStatus.완료);
        }

        Academy academy = academyRepo.findById(scope.academyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.setAcademy(academy);

        if (req.getTeacherUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "담당 상담교사를 지정해 주세요.");
        }
        User teacher = userRepo.findByIdAndAcademy_Id(req.getTeacherUserId(), scope.academyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "담당 상담교사를 찾을 수 없습니다."));
        c.setTeacher(teacher);

        if (kind == ConsultKind.재원생) {
            if (req.getStudentId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상담 대상 학생을 선택해 주세요.");
            }
            Student stu = stuRepo.findById(req.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다."));
            if (!StudentAdminService.belongsToAcademy(stu, scope.academyId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
            scope.requireStudent(stu);
            c.setStudent(stu);
            c.setProspectName(null);
            c.setProspectPhone(null);
            c.setProspectGrade(null);
            c.setProspectParentName(null);
        } else {
            if (req.getProspectName() == null || req.getProspectName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "신규 상담 대상 이름을 입력해 주세요.");
            }
            c.setStudent(null);
            c.setProspectName(req.getProspectName().trim());
            c.setProspectPhone(blankToNull(req.getProspectPhone()));
            c.setProspectGrade(blankToNull(req.getProspectGrade()));
            c.setProspectParentName(blankToNull(req.getProspectParentName()));
        }
    }

    private Consultation loadOwned(Long academyId, Long id) {
        Consultation c = consultRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "상담 기록을 찾을 수 없습니다."));
        Academy a = c.resolveAcademy();
        if (a == null || !a.getId().equals(academyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return c;
    }

    private static ConsultKind kindOf(Consultation c) {
        return c.getKind() != null ? c.getKind() : ConsultKind.재원생;
    }

    private static ConsultKind parseKind(String kind) {
        ConsultKind parsed = parseKindOrNull(kind);
        if (parsed == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상담 구분은 재원생 또는 신규여야 합니다.");
        }
        return parsed;
    }

    private static ConsultKind parseKindOrNull(String kind) {
        if (kind == null || kind.isBlank() || "all".equalsIgnoreCase(kind)) return null;
        try {
            return ConsultKind.valueOf(kind.trim());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상담 구분은 재원생 또는 신규여야 합니다.");
        }
    }

    private static ConsultStatus parseStatus(String status) {
        try {
            return ConsultStatus.valueOf(status.trim());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상담 상태가 올바르지 않습니다.");
        }
    }

    private static LocalDate monthStart(int year, int month) {
        return LocalDate.of(year, month, 1);
    }

    private static LocalDate monthEnd(int year, int month) {
        return monthStart(year, month).withDayOfMonth(monthStart(year, month).lengthOfMonth());
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private static boolean sameGuardian(Student a, Student b) {
        Academy aa = a.resolveAcademy();
        Academy ba = b.resolveAcademy();
        if (aa == null || ba == null || !aa.getId().equals(ba.getId())) return false;
        return a.resolveLoginPhone().equals(b.resolveLoginPhone());
    }
}
