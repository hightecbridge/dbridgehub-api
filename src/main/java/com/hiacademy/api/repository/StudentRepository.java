package com.hiacademy.api.repository;

import com.hiacademy.api.entity.Student;
import com.hiacademy.api.entity.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findAllByParent_Id(Long parentId);
    List<Student> findAllByClassroom_Id(Long classroomId);

    @Query("SELECT s FROM Student s JOIN s.parent p WHERE p.academy.id = :academyId")
    List<Student> findAllByAcademyId(@Param("academyId") Long academyId);

    /** 공지 대상(반 이름) 필터 시 classroom 로딩 */
    @Query("SELECT DISTINCT s FROM Student s JOIN FETCH s.parent p LEFT JOIN FETCH s.classroom c WHERE p.academy.id = :academyId")
    List<Student> findAllByAcademyIdWithClassroom(@Param("academyId") Long academyId);

    /** 퇴원 제외(재원·휴원) — 등록 한도 집계 */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.parent.academy.id = :academyId AND s.status <> :withdrawn")
    long countByAcademyIdExcludingWithdrawn(@Param("academyId") Long academyId, @Param("withdrawn") StudentStatus withdrawn);
}
