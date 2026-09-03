package com.hiacademy.api.repository;

import com.hiacademy.api.entity.ExamClassroomInput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamClassroomInputRepository extends JpaRepository<ExamClassroomInput, Long> {
    List<ExamClassroomInput> findAllByExam_Id(Long examId);
    Optional<ExamClassroomInput> findByExam_IdAndClassroom_Id(Long examId, Long classroomId);
}
