package com.hiacademy.api.service;

import com.hiacademy.api.dto.request.ParentLoginRequest;
import com.hiacademy.api.dto.request.ParentSignupRequest;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.menu.MenuSettings;
import com.hiacademy.api.repository.*;
import com.hiacademy.api.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class ParentAuthService {
    private static final Logger log = LoggerFactory.getLogger(ParentAuthService.class);

    private final StudentRepository studentRepo;
    private final AcademyRepository academyRepo;
    private final FeeRecordRepository feeRepo;
    private final HomeworkRecordRepository hwRecordRepo;
    private final CalendarEventRepository eventRepo;
    private final NoticeRepository noticeRepo;
    private final ClassNoticeRepository classNoticeRepo;
    private final GradeService gradeSvc;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public ParentAuthService(
            StudentRepository studentRepo,
            AcademyRepository academyRepo,
            FeeRecordRepository feeRepo,
            HomeworkRecordRepository hwRecordRepo,
            CalendarEventRepository eventRepo,
            NoticeRepository noticeRepo,
            ClassNoticeRepository classNoticeRepo,
            GradeService gradeSvc,
            PasswordEncoder encoder,
            JwtUtil jwt) {
        this.studentRepo = studentRepo;
        this.academyRepo = academyRepo;
        this.feeRepo = feeRepo;
        this.hwRecordRepo = hwRecordRepo;
        this.eventRepo = eventRepo;
        this.noticeRepo = noticeRepo;
        this.classNoticeRepo = classNoticeRepo;
        this.gradeSvc = gradeSvc;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse login(ParentLoginRequest req) {
        String phone = normalizePhone(req.getPhone());
        String rawPassword = req.getPassword();
        log.info("[ParentAuth] login attempt phone={}", phone);

        if (phone == null) {
            throw unauthorized();
        }

        List<Student> matches = studentRepo.findAllByLoginPhone(phone);
        if (matches.isEmpty()) {
            throw unauthorized();
        }

        Map<Long, List<Student>> byAcademy = groupByAcademy(matches);
        if (byAcademy.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "학원 정보를 찾을 수 없습니다.");
        }

        Long academyId = req.getAcademyId();
        if (academyId != null) {
            List<Student> inAcademy = byAcademy.get(academyId);
            if (inAcademy == null || inAcademy.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "선택한 학원에서 이 번호를 찾을 수 없습니다.");
            }
            return issueLogin(pickAnchor(inAcademy, rawPassword));
        }

        if (byAcademy.size() == 1) {
            List<Student> inAcademy = byAcademy.values().iterator().next();
            return issueLogin(pickAnchor(inAcademy, rawPassword));
        }

        if (matches.stream().noneMatch(s -> passwordMatches(s, rawPassword))) {
            throw unauthorized();
        }

        List<ParentLoginAcademyOption> options = byAcademy.values().stream()
            .map(students -> {
                Academy academy = students.get(0).resolveAcademy();
                return ParentLoginAcademyOption.builder()
                    .id(academy.getId())
                    .name(academy.getName())
                    .address(academy.getAddress())
                    .studentCount(students.size())
                    .build();
            })
            .sorted(Comparator.comparing(ParentLoginAcademyOption::getName, Comparator.nullsLast(String::compareTo)))
            .toList();

        return AuthResponse.builder()
            .type("parent")
            .needsAcademySelection(true)
            .academies(options)
            .build();
    }

    private static Map<Long, List<Student>> groupByAcademy(List<Student> students) {
        Map<Long, List<Student>> byAcademy = new LinkedHashMap<>();
        for (Student student : students) {
            Academy academy = student.resolveAcademy();
            if (academy == null || academy.getId() == null) continue;
            byAcademy.computeIfAbsent(academy.getId(), id -> new ArrayList<>()).add(student);
        }
        return byAcademy;
    }

    /** 같은 학원 형제·자매는 한 로그인. 비밀번호가 맞는 학생을 JWT 대표로 쓴다. */
    private Student pickAnchor(List<Student> siblings, String rawPassword) {
        return siblings.stream()
            .filter(s -> passwordMatches(s, rawPassword))
            .findFirst()
            .orElseThrow(this::unauthorized);
    }

    private boolean passwordMatches(Student student, String rawPassword) {
        return student.getLoginPassword() != null
            && rawPassword != null
            && encoder.matches(rawPassword, student.getLoginPassword());
    }

    private AuthResponse issueLogin(Student student) {
        Academy academy = student.resolveAcademy();
        return AuthResponse.builder()
            .token(jwt.generateGuardianToken(student))
            .type("parent").id(student.getId())
            .name(student.resolveParentName())
            .phone(student.resolveParentPhone())
            .createdAt(student.getCreatedAt())
            .academy(Mapper.toAcademyInfo(academy))
            .build();
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "전화번호 또는 비밀번호가 올바르지 않습니다.");
    }

    public AuthResponse signup(ParentSignupRequest req) {
        String phone = normalizePhone(req.getPhone());
        if (phone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "전화번호를 올바르게 입력해 주세요.");
        }
        boolean exists = !studentRepo.findAllByAcademy_IdAndLoginPhone(req.getAcademyId(), phone).isEmpty();
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이 학원에 이미 등록된 전화번호입니다.");
        }

        Academy academy = academyRepo.getReferenceById(req.getAcademyId());
        Student student = studentRepo.save(Student.builder()
            .name(req.getName())
            .grade("미등록")
            .parentName(req.getName())
            .parentPhone(phone)
            .loginPhone(phone)
            .loginPassword(encoder.encode(req.getPassword()))
            .status(StudentStatus.재원)
            .kakaoLinked(false)
            .academy(academy)
            .build());

        return AuthResponse.builder()
            .token(jwt.generateGuardianToken(student))
            .type("parent").id(student.getId())
            .name(student.resolveParentName())
            .phone(student.resolveParentPhone())
            .createdAt(student.getCreatedAt())
            .academy(Mapper.toAcademyInfo(academy))
            .build();
    }

    @Transactional(readOnly = true)
    public ParentHomeResponse getHome(Long studentId) {
        Student anchor = studentRepo.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Academy academy = anchor.resolveAcademy();
        if (academy == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "학원 정보를 찾을 수 없습니다.");
        }
        String loginPhone = anchor.resolveLoginPhone();
        List<Student> siblings = studentRepo.findSiblingsByAcademyAndLoginPhone(academy.getId(), loginPhone);
        if (siblings.isEmpty()) siblings = List.of(anchor);

        var menu = MenuSettings.merge(academy.getMenuSettingsJson());
        boolean showHomework = MenuSettings.parentVisible(menu, "homework");
        boolean showRegularGrades = MenuSettings.parentVisible(menu, "examsRegular");
        boolean showDailyGrades = MenuSettings.parentVisible(menu, "examsDaily");
        boolean showNotice = MenuSettings.parentVisible(menu, "notice");
        boolean showClassNotice = MenuSettings.parentVisible(menu, "classNotice");
        boolean showCalendar = MenuSettings.parentVisible(menu, "calendar");

        int ym = Integer.parseInt(LocalDate.now().toString().replace("-", "").substring(0, 6));
        List<ParentHomeResponse.StudentSummary> summaries = siblings.stream().map(st -> {
            var fees = feeRepo.findAllByStudent_IdAndYearMonth(st.getId(), ym);
            long hwPending = showHomework
                ? hwRecordRepo.findAllByStudentIdOrderByDateDesc(st.getId()).stream().filter(r -> !r.isDone()).count()
                : 0;
            var recent = List.<GradeItemResponse>of();
            if (showRegularGrades || showDailyGrades) {
                var grades = gradeSvc.parentHistory(st, showRegularGrades, showDailyGrades);
                recent = grades.getItems() == null ? List.of() : grades.getItems().stream().limit(3).toList();
            }
            return ParentHomeResponse.StudentSummary.builder()
                .id(st.getId()).name(st.getName()).grade(st.getGrade())
                .className(st.getClassroom() != null ? st.getClassroom().getName() : null)
                .status(st.getStatus() != null ? st.getStatus().name() : null)
                .fees(fees.stream().map(Mapper::toFee).toList())
                .hwPendingCount((int) hwPending)
                .recentGrades(recent)
                .build();
        }).toList();

        var notices = List.<NoticeResponse>of();
        if (showNotice) {
            var noticeEntities = noticeRepo.findAllByAcademy_IdOrderByCreatedAtDesc(academy.getId());
            noticeEntities.forEach(n -> {
                org.hibernate.Hibernate.initialize(n.getTargets());
                org.hibernate.Hibernate.initialize(n.getAttachments());
            });
            notices = noticeEntities.stream().limit(30).map(Mapper::toNotice).toList();
        }

        var classNotices = List.<NoticeResponse>of();
        if (showClassNotice) {
            java.util.Set<String> childClasses = siblings.stream()
                .map(st -> st.getClassroom() != null ? st.getClassroom().getName() : null)
                .filter(name -> name != null && !name.isBlank())
                .collect(java.util.stream.Collectors.toSet());
            var classNoticeEntities = classNoticeRepo.findAllByAcademy_IdOrderByCreatedAtDesc(academy.getId());
            classNoticeEntities.forEach(n -> {
                org.hibernate.Hibernate.initialize(n.getTargets());
                org.hibernate.Hibernate.initialize(n.getAttachments());
            });
            classNotices = classNoticeEntities.stream()
                .filter(n -> visibleToParent(n.getTargets(), childClasses))
                .limit(30)
                .map(Mapper::toClassNotice)
                .toList();
        }

        var events = showCalendar
            ? eventRepo.findByAcademyAndDateRange(
                academy.getId(), LocalDate.now(), LocalDate.now().plusMonths(2))
                .stream().limit(5).map(Mapper::toEvent).toList()
            : List.<EventResponse>of();
        return ParentHomeResponse.builder()
            .academy(Mapper.toAcademyInfo(academy))
            .students(summaries)
            .recentNotices(notices)
            .recentClassNotices(classNotices)
            .upcomingEvents(events).build();
    }

    private static boolean visibleToParent(List<String> targets, java.util.Set<String> childClasses) {
        if (targets == null || targets.isEmpty()) return false;
        for (String t : targets) {
            if (t == null) continue;
            if ("전체".equals(t.trim())) return true;
            if (childClasses.contains(t.trim())) return true;
        }
        return false;
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return null;
        String normalized = phone.replaceAll("[^0-9]", "");
        return normalized.isBlank() ? null : normalized;
    }
}
