package com.hiacademy.api.service;
import com.hiacademy.api.dto.request.ParentLoginRequest;
import com.hiacademy.api.dto.request.ParentSignupRequest;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.*;
import com.hiacademy.api.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.List;

@Service @Transactional
public class ParentAuthService {
    private static final Logger log = LoggerFactory.getLogger(ParentAuthService.class);

    private final ParentRepository         parentRepo;
    private final AcademyRepository       academyRepo;
    private final FeeRecordRepository      feeRepo;
    private final HomeworkRecordRepository hwRecordRepo;
    private final CalendarEventRepository  eventRepo;
    private final NoticeRepository         noticeRepo;
    private final PasswordEncoder          encoder;
    private final JwtUtil                  jwt;
    public ParentAuthService(ParentRepository p, AcademyRepository a, FeeRecordRepository f,
            HomeworkRecordRepository hw, CalendarEventRepository e,
            NoticeRepository n, PasswordEncoder enc, JwtUtil j) {
        parentRepo=p; academyRepo=a; feeRepo=f; hwRecordRepo=hw; eventRepo=e; noticeRepo=n; encoder=enc; jwt=j;
    }
    public AuthResponse login(ParentLoginRequest req) {
        String phone = req.getPhone();
        String rawPassword = req.getPassword();
        log.info("[ParentAuth] login attempt phone={}", phone);
        log.info("[ParentAuth] received pwLen={}", rawPassword != null ? rawPassword.length() : null);

        Parent parent = parentRepo.findByLoginPhone(phone)
            .orElseThrow(() -> {
                log.info("[ParentAuth] parent not found for phone={}", phone);
                return new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "전화번호 또는 비밀번호가 올바르지 않습니다."
                );
            });

        // 테스트 용도: 비밀번호 검증을 임시로 스킵합니다.
        // (운영에서는 반드시 비밀번호 검증 로직을 되돌려야 합니다.)
        log.info("[ParentAuth] skip password check for phone={} (testing mode)", phone);
        return AuthResponse.builder().token(jwt.generateParentToken(parent))
            .type("parent").id(parent.getId()).name(parent.getName()).phone(parent.getPhone())
            .createdAt(parent.getCreatedAt())
            .academy(Mapper.toAcademyInfo(parent.getAcademy())).build();
    }

    public AuthResponse signup(ParentSignupRequest req) {
        String phone = req.getPhone();
        log.info("[ParentAuth] signup attempt academyId={} phoneLen={}", req.getAcademyId(), phone != null ? phone.length() : null);

        if (parentRepo.existsByLoginPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 전화번호입니다.");
        }

        Parent parent = parentRepo.save(Parent.builder()
            .name(req.getName())
            .phone(phone)
            .loginPhone(phone)
            .loginPassword(encoder.encode(req.getPassword()))
            .kakaoLinked(false)
            .academy(academyRepo.getReferenceById(req.getAcademyId()))
            .build());

        return AuthResponse.builder().token(jwt.generateParentToken(parent))
            .type("parent").id(parent.getId()).name(parent.getName()).phone(parent.getPhone())
            .createdAt(parent.getCreatedAt())
            .academy(Mapper.toAcademyInfo(parent.getAcademy())).build();
    }

    @Transactional(readOnly=true)
    public ParentHomeResponse getHome(Long parentId) {
        Parent parent = parentRepo.findById(parentId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        Academy academy = parent.getAcademy();
        int ym = Integer.parseInt(LocalDate.now().toString().replace("-","").substring(0,6));
        List<ParentHomeResponse.StudentSummary> summaries = parent.getStudents().stream().map(st -> {
            var fees = feeRepo.findAllByStudent_IdAndYearMonth(st.getId(), ym);
            long hwPending = hwRecordRepo.findAllByStudentIdOrderByDateDesc(st.getId())
                .stream().filter(r->!r.isDone()).count();
            return ParentHomeResponse.StudentSummary.builder()
                .id(st.getId()).name(st.getName()).grade(st.getGrade())
                .className(st.getClassroom()!=null?st.getClassroom().getName():null)
                .status(st.getStatus()!=null?st.getStatus().name():null)
                .fees(fees.stream().map(Mapper::toFee).toList())
                .hwPendingCount((int)hwPending).build();
        }).toList();
        // 공지사항: 홈에서도 '전체보기'에서 전부 노출될 수 있도록 limit 제거
        var notices = noticeRepo.findAllByAcademy_IdOrderByCreatedAtDesc(academy.getId())
            .stream().map(Mapper::toNotice).toList();
        var events = eventRepo.findByAcademyAndDateRange(
            academy.getId(), LocalDate.now(), LocalDate.now().plusMonths(2))
            .stream().limit(5).map(Mapper::toEvent).toList();
        return ParentHomeResponse.builder().academy(Mapper.toAcademyInfo(academy))
            .students(summaries).recentNotices(notices).upcomingEvents(events).build();
    }
}
