package com.hiacademy.api.repository;

import com.hiacademy.api.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findAllByClassroom_IdOrderByExamDateDescIdDesc(Long classroomId);

    List<Exam> findAllByAcademy_IdOrderByExamDateDescIdDesc(Long academyId);

    Optional<Exam> findByIdAndClassroom_Id(Long id, Long classroomId);

    Optional<Exam> findByIdAndAcademy_Id(Long id, Long academyId);

    @Query("""
        SELECT DISTINCT e FROM Exam e
        LEFT JOIN FETCH e.scores s
        LEFT JOIN FETCH s.student st
        LEFT JOIN FETCH st.classroom
        WHERE e.id = :id AND e.academy.id = :academyId
        """)
    Optional<Exam> findDetailByIdAndAcademyId(@Param("id") Long id, @Param("academyId") Long academyId);

    @Query("""
        SELECT DISTINCT e FROM Exam e
        LEFT JOIN FETCH e.scores s
        LEFT JOIN FETCH s.student
        WHERE e.id = :id AND e.classroom.id = :classroomId
        """)
    Optional<Exam> findDetailByIdAndClassroomId(@Param("id") Long id, @Param("classroomId") Long classroomId);

    @Query("""
        SELECT DISTINCT e FROM Exam e
        LEFT JOIN FETCH e.scores s
        LEFT JOIN FETCH s.student
        WHERE e.classroom.id = :classroomId
        ORDER BY e.examDate DESC, e.id DESC
        """)
    List<Exam> findAllWithScoresByClassroomId(@Param("classroomId") Long classroomId);
}
