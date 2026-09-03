package com.hiacademy.api.service;

import com.hiacademy.api.billing.BillingPlanLimits;
import com.hiacademy.api.dto.request.FeeUpdateRequest;
import com.hiacademy.api.dto.request.StudentRequest;
import com.hiacademy.api.dto.response.FeeResponse;
import com.hiacademy.api.dto.response.StudentResponse;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class StudentAdminService {
    private final StudentRepository studentRepo;
    private final ClassRoomRepository clsRepo;
    private final FeeRecordRepository feeRepo;
    private final AttendRecordRepository attendRecordRepo;
    private final HomeworkRecordRepository homeworkRecordRepo;
    private final ConsultationRepository consultationRepo;
    private final ParentPushTokenRepository pushTokenRepo;
    private final AcademyRepository academyRepo;
    private final PasswordEncoder encoder;
    private final AdminAccessService access;

    public StudentAdminService(
            StudentRepository studentRepo,
            ClassRoomRepository clsRepo,
            FeeRecordRepository feeRepo,
            AttendRecordRepository attendRecordRepo,
            HomeworkRecordRepository homeworkRecordRepo,
            ConsultationRepository consultationRepo,
            ParentPushTokenRepository pushTokenRepo,
            AcademyRepository academyRepo,
            PasswordEncoder encoder,
            AdminAccessService access) {
        this.studentRepo = studentRepo;
        this.clsRepo = clsRepo;
        this.feeRepo = feeRepo;
        this.attendRecordRepo = attendRecordRepo;
        this.homeworkRecordRepo = homeworkRecordRepo;
        this.consultationRepo = consultationRepo;
        this.pushTokenRepo = pushTokenRepo;
        this.academyRepo = academyRepo;
        this.encoder = encoder;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> listStudents(Authentication auth, Integer yearMonth) {
        AdminAccessService.Scope scope = access.resolve(auth);
        int ym = Mapper.normalizeYearMonth(yearMonth);
        return studentRepo.findAllByAcademyIdWithClassroom(scope.academyId()).stream()
            .filter(scope::allowsStudent)
            .map(s -> Mapper.toStudent(s, ym))
            .toList();
    }

    public StudentResponse createStudent(Authentication auth, StudentRequest req) {
        AdminAccessService.Scope scope = access.resolve(auth);
        scope.requireClassroom(req.getClassroomId());
        Long academyId = scope.academyId();
        String parentPhone = normalizePhone(req.getParentPhone());
        String loginPhone = normalizePhone(req.getLoginPhone() != null ? req.getLoginPhone() : req.getParentPhone());
        if (parentPhone == null || loginPhone == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학부모 전화번호를 올바르게 입력해 주세요.");
        }
        Academy academy = academyRepo.findById(academyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertStudentLimit(academy);

        ClassRoom cls = clsRepo.findByIdAndAcademy_Id(req.getClassroomId(), academyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "반을 찾을 수 없습니다."));

        String encodedPw = req.getLoginPassword() != null && !req.getLoginPassword().isBlank()
            ? encoder.encode(req.getLoginPassword())
            : encoder.encode("0000");

        Student st = studentRepo.save(Student.builder()
            .name(req.getName().trim())
            .grade(req.getGrade())
            .birthDate(req.getBirthDate())
            .phone(normalizePhone(req.getPhone()))
            .status(req.getStatus() != null ? StudentStatus.valueOf(req.getStatus()) : StudentStatus.재원)
            .parentName(req.getParentName().trim())
            .parentPhone(parentPhone)
            .loginPhone(loginPhone)
            .loginPassword(encodedPw)
            .badgeColor(req.getBadgeColor() != null ? req.getBadgeColor() : "#DBEAFE")
            .badgeTextColor(req.getBadgeTextColor() != null ? req.getBadgeTextColor() : "#1D4ED8")
            .kakaoLinked(req.getKakaoLinked() != null && req.getKakaoLinked())
            .academy(academy)
            .classroom(cls)
            .build());

        int ym = Integer.parseInt(LocalDate.now().toString().replace("-", "").substring(0, 6));
        feeRepo.save(FeeRecord.builder().label("수업료").amount(cls.getTuitionFee()).paid(false).yearMonth(ym).student(st).build());
        feeRepo.save(FeeRecord.builder().label("교재비").amount(cls.getBookFee()).paid(false).yearMonth(ym).student(st).build());
        return Mapper.toStudent(studentRepo.findById(st.getId()).orElseThrow());
    }

    public StudentResponse updateStatus(Authentication auth, Long studentId, String status) {
        AdminAccessService.Scope scope = access.resolve(auth);
        Student st = studentRepo.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다."));
        if (!belongsToAcademy(st, scope.academyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        scope.requireStudent(st);
        StudentStatus next;
        try {
            next = StudentStatus.valueOf(status.trim());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "재원 상태는 재원, 휴원, 퇴원 중 하나여야 합니다.");
        }
        StudentStatus prev = st.getStatus();
        if (next == StudentStatus.퇴원 && prev != StudentStatus.퇴원) {
            st.setWithdrawnAt(java.time.LocalDateTime.now());
        }
        if (prev == StudentStatus.퇴원 && next != StudentStatus.퇴원) {
            Academy academy = st.resolveAcademy();
            if (academy != null) assertStudentLimit(academy);
        }
        st.setStatus(next);
        return Mapper.toStudent(studentRepo.save(st));
    }

    /** 학생은 삭제하지 않고 퇴원 처리하여 상담·수납 기록을 유지한다. */
    public StudentResponse withdrawStudent(Authentication auth, Long studentId) {
        return updateStatus(auth, studentId, StudentStatus.퇴원.name());
    }

    public void deleteStudent(Authentication auth, Long studentId) {
        withdrawStudent(auth, studentId);
    }

    public FeeResponse updateFee(Authentication auth, Long studentId, FeeUpdateRequest req) {
        AdminAccessService.Scope scope = access.resolve(auth);
        Student st = studentRepo.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!belongsToAcademy(st, scope.academyId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        scope.requireStudent(st);
        int ym = req.getYearMonth() > 0
            ? req.getYearMonth()
            : Integer.parseInt(LocalDate.now().toString().replace("-", "").substring(0, 6));
        FeeRecord fee = feeRepo.findAllByStudent_IdAndYearMonth(studentId, ym)
            .stream().filter(f -> f.getLabel().equals(req.getLabel())).findFirst()
            .orElse(null);
        if (fee == null) {
            if (st.getClassroom() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "반 정보가 없어 수납 항목을 생성할 수 없습니다.");
            }
            int amount = "수업료".equals(req.getLabel())
                ? st.getClassroom().getTuitionFee()
                : st.getClassroom().getBookFee();
            fee = FeeRecord.builder()
                .label(req.getLabel())
                .amount(amount)
                .paid(req.isPaid())
                .yearMonth(ym)
                .student(st)
                .build();
            applyFeePaymentFields(fee, req);
            return Mapper.toFee(feeRepo.save(fee));
        }
        applyFeePaymentFields(fee, req);
        return Mapper.toFee(feeRepo.save(fee));
    }

    static boolean belongsToAcademy(Student st, Long academyId) {
        Academy a = st.resolveAcademy();
        return a != null && a.getId().equals(academyId);
    }

    private void assertStudentLimit(Academy academy) {
        long rosterCount = studentRepo.countByAcademyIdExcludingWithdrawn(academy.getId(), StudentStatus.퇴원);
        if (rosterCount == 0) {
            rosterCount = studentRepo.countByAcademyIdExcludingWithdrawnLegacy(academy.getId(), StudentStatus.퇴원);
        }
        int max = BillingPlanLimits.maxStudents(academy.getBillingPlanId());
        if (!BillingPlanLimits.isUnlimited(max) && rosterCount >= max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "요금제에 따른 학생 등록 한도(" + max + "명)에 도달했습니다. 상위 요금제로 변경해 주세요.");
        }
    }

    private static void applyFeePaymentFields(FeeRecord fee, FeeUpdateRequest req) {
        fee.setPaid(req.isPaid());
        if (req.isPaid()) {
            if (req.getPaidAt() != null && !req.getPaidAt().isBlank()) {
                fee.setPaidAt(LocalDate.parse(req.getPaidAt().trim()));
            } else {
                fee.setPaidAt(LocalDate.now());
            }
            String method = req.getPaymentMethod();
            fee.setPaymentMethod(method != null && !method.isBlank() ? method.trim() : null);
        } else {
            fee.setPaidAt(null);
            fee.setPaymentMethod(null);
        }
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return null;
        String normalized = phone.replaceAll("[^0-9]", "");
        return normalized.isBlank() ? null : normalized;
    }
}
