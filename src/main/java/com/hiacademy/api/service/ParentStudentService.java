package com.hiacademy.api.service;
import com.hiacademy.api.billing.BillingPlanLimits;
import com.hiacademy.api.dto.request.*;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@Service @Transactional
public class ParentStudentService {
    private final ParentRepository    parentRepo;
    private final StudentRepository   studentRepo;
    private final ClassRoomRepository clsRepo;
    private final FeeRecordRepository feeRepo;
    private final AcademyRepository   academyRepo;
    private final PasswordEncoder     encoder;
    public ParentStudentService(ParentRepository p, StudentRepository s, ClassRoomRepository c,
            FeeRecordRepository f, AcademyRepository a, PasswordEncoder e) {
        parentRepo=p; studentRepo=s; clsRepo=c; feeRepo=f; academyRepo=a; encoder=e;
    }
    @Transactional(readOnly=true)
    public List<ParentResponse> listParents(Long academyId) {
        return parentRepo.findAllByAcademy_IdOrderByCreatedAtDesc(academyId).stream().map(Mapper::toParent).toList();
    }
    public ParentResponse createParent(Long academyId, ParentRequest req) {
        if (req.getLoginPhone()!=null && parentRepo.existsByLoginPhone(req.getLoginPhone()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"이미 등록된 전화번호입니다.");
        Academy a = academyRepo.getReferenceById(academyId);
        return Mapper.toParent(parentRepo.save(Parent.builder()
            .name(req.getName()).phone(req.getPhone())
            .badgeColor(req.getBadgeColor()).badgeTextColor(req.getBadgeTextColor())
            .kakaoLinked(req.isKakaoLinked()).loginPhone(req.getLoginPhone())
            .loginPassword(req.getLoginPassword()!=null?encoder.encode(req.getLoginPassword()):null)
            .academy(a).build()));
    }
    public StudentResponse addStudent(Long academyId, Long parentId, StudentRequest req) {
        Parent parent = parentRepo.findByIdAndAcademy_Id(parentId,academyId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        Academy academy = academyRepo.findById(academyId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        long rosterCount = studentRepo.countByAcademyIdExcludingWithdrawn(academyId, StudentStatus.퇴원);
        int max = BillingPlanLimits.maxStudents(academy.getBillingPlanId());
        if (!BillingPlanLimits.isUnlimited(max) && rosterCount >= max) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "요금제에 따른 학생 등록 한도(" + max + "명)에 도달했습니다. 상위 요금제로 변경해 주세요.");
        }
        ClassRoom cls = clsRepo.findByIdAndAcademy_Id(req.getClassroomId(),academyId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"반을 찾을 수 없습니다."));
        Student st = studentRepo.save(Student.builder()
            .name(req.getName()).grade(req.getGrade()).birthDate(req.getBirthDate())
            .status(req.getStatus()!=null?StudentStatus.valueOf(req.getStatus()):StudentStatus.재원)
            .parent(parent).classroom(cls).build());
        int ym = Integer.parseInt(LocalDate.now().toString().replace("-","").substring(0,6));
        feeRepo.save(FeeRecord.builder().label("수업료").amount(cls.getTuitionFee()).paid(false).yearMonth(ym).student(st).build());
        feeRepo.save(FeeRecord.builder().label("교재비").amount(cls.getBookFee()).paid(false).yearMonth(ym).student(st).build());
        return Mapper.toStudent(studentRepo.findById(st.getId()).orElseThrow());
    }
    public FeeResponse updateFee(Long academyId, Long studentId, FeeUpdateRequest req) {
        Student st = studentRepo.findById(studentId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!st.getParent().getAcademy().getId().equals(academyId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        FeeRecord fee = feeRepo.findAllByStudent_IdAndYearMonth(studentId,req.getYearMonth())
            .stream().filter(f->f.getLabel().equals(req.getLabel())).findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        fee.setPaid(req.isPaid());
        return Mapper.toFee(feeRepo.save(fee));
    }
}
