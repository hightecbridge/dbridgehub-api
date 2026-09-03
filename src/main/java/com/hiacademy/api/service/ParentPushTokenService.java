package com.hiacademy.api.service;

import com.hiacademy.api.entity.Parent;
import com.hiacademy.api.entity.ParentPushToken;
import com.hiacademy.api.entity.Student;
import com.hiacademy.api.repository.ParentPushTokenRepository;
import com.hiacademy.api.repository.ParentRepository;
import com.hiacademy.api.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ParentPushTokenService {
    private static final Logger log = LoggerFactory.getLogger(ParentPushTokenService.class);

    private final ParentRepository parentRepo;
    private final StudentRepository studentRepo;
    private final ParentPushTokenRepository tokenRepo;

    @Transactional
    public void register(Long subjectId, String expoPushToken) {
        if (expoPushToken == null || expoPushToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expoPushToken이 필요합니다.");
        }
        String token = expoPushToken.trim();
        Student student = studentRepo.findById(subjectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var academy = student.resolveAcademy();
        if (academy == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "학원 정보가 없습니다.");
        }

        tokenRepo.findByExpoPushToken(token).ifPresentOrElse(
            existing -> {
                existing.setStudent(student);
                existing.setAcademy(academy);
                if (existing.getToken() == null || existing.getToken().isBlank()) {
                    existing.setToken(token);
                }
                tokenRepo.save(existing);
                log.info("[ParentPushToken] updated token for studentId={}", subjectId);
            },
            () -> {
                ParentPushToken row = ParentPushToken.builder()
                    .student(student)
                    .academy(academy)
                    .expoPushToken(token)
                    .token(token)
                    .build();
                if (student.getParent() != null) {
                    row.setParent(student.getParent());
                }
                tokenRepo.save(row);
                log.info("[ParentPushToken] registered token for studentId={}", subjectId);
            }
        );
    }

    @Transactional
    public void unregisterAll(Long subjectId) {
        tokenRepo.deleteAllByStudent_Id(subjectId);
        Parent legacy = parentRepo.findById(subjectId).orElse(null);
        if (legacy != null) {
            tokenRepo.deleteAllByParent_Id(subjectId);
        }
    }
}
