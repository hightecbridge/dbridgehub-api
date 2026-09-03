package com.hiacademy.api.repository;

import com.hiacademy.api.entity.ExamScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamScoreRepository extends JpaRepository<ExamScore, Long> {
    List<ExamScore> findAllByExam_Id(Long examId);

    Optional<ExamScore> findByExam_IdAndStudent_Id(Long examId, Long studentId);

    @Query("""
        SELECT s FROM ExamScore s
        JOIN FETCH s.exam e
        JOIN FETCH e.classroom
        JOIN FETCH s.student
        WHERE s.student.id = :studentId
        ORDER BY e.examDate DESC, e.id DESC
        """)
    List<ExamScore> findAllByStudentIdWithExam(@Param("studentId") Long studentId);

    @Query("""
        SELECT s FROM ExamScore s
        JOIN FETCH s.student
        WHERE s.exam.id IN :examIds
        """)
    List<ExamScore> findAllByExam_IdIn(@Param("examIds") Collection<Long> examIds);
}
