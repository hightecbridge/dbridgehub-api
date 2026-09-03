package com.hiacademy.api.service;

import com.hiacademy.api.controller.AuthHelper;
import com.hiacademy.api.entity.ClassRoom;
import com.hiacademy.api.entity.Student;
import com.hiacademy.api.repository.ClassRoomRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminAccessService {
    private final ClassRoomRepository clsRepo;

    public AdminAccessService(ClassRoomRepository clsRepo) {
        this.clsRepo = clsRepo;
    }

    public Scope resolve(Authentication auth) {
        Long academyId = AuthHelper.academyId(auth);
        Long userId = AuthHelper.subjectId(auth);
        boolean director = AuthHelper.isDirector(auth);
        Set<Long> classroomIds = director
            ? Set.of()
            : clsRepo.findAllByTeacherUser_IdAndAcademy_Id(userId, academyId).stream()
                .map(ClassRoom::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new Scope(academyId, userId, director, classroomIds);
    }

    public void requireDirector(Authentication auth) {
        resolve(auth).requireDirector();
    }

    public record Scope(Long academyId, Long userId, boolean director, Set<Long> classroomIds) {
        public void requireDirector() {
            if (!director) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "원장만 이용할 수 있습니다.");
            }
        }

        public void requireClassroom(Long classroomId) {
            if (director) return;
            if (classroomId == null || !classroomIds.contains(classroomId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 반만 관리할 수 있습니다.");
            }
        }

        public boolean allowsClassroom(Long classroomId) {
            return director || (classroomId != null && classroomIds.contains(classroomId));
        }

        public boolean allowsStudent(Student s) {
            if (director) return true;
            return s.getClassroom() != null && classroomIds.contains(s.getClassroom().getId());
        }

        public void requireStudent(Student s) {
            if (!allowsStudent(s)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 반 학생만 관리할 수 있습니다.");
            }
        }
    }
}
