package com.hiacademy.api.service;

import com.hiacademy.api.controller.AuthHelper;
import com.hiacademy.api.entity.Academy;
import com.hiacademy.api.entity.Student;
import com.hiacademy.api.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 학부모 JWT(대표 학생 id)로 요청한 studentId 가 본인 자녀인지 API 레벨에서 검증한다.
 */
@Service
@Transactional(readOnly = true)
public class ParentAccessService {
    private final StudentRepository studentRepo;

    public ParentAccessService(StudentRepository studentRepo) {
        this.studentRepo = studentRepo;
    }

    public Student requireChild(Authentication auth, Long requestedStudentId) {
        Long anchorId = AuthHelper.subjectId(auth);
        Long academyId = AuthHelper.academyId(auth);
        if (requestedStudentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학생을 지정해 주세요.");
        }
        Student child = studentRepo.findById(requestedStudentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학생을 찾을 수 없습니다."));
        Student anchor = studentRepo.findById(anchorId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학부모 계정을 찾을 수 없습니다."));
        if (!sameGuardian(anchor, child)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 자녀의 정보만 조회할 수 있습니다.");
        }
        Academy academy = child.resolveAcademy();
        if (academy == null || academyId == null || !academy.getId().equals(academyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 자녀의 정보만 조회할 수 있습니다.");
        }
        return child;
    }

    static boolean sameGuardian(Student a, Student b) {
        if (a == null || b == null) return false;
        if (a.getId() != null && a.getId().equals(b.getId())) return true;
        Academy aa = a.resolveAcademy();
        Academy ba = b.resolveAcademy();
        if (aa == null || ba == null || aa.getId() == null || !aa.getId().equals(ba.getId())) return false;
        String ap = a.resolveLoginPhone();
        String bp = b.resolveLoginPhone();
        if (ap == null || ap.isBlank() || bp == null || bp.isBlank()) return false;
        return ap.equals(bp);
    }
}
