package com.hiacademy.api.repository;

import com.hiacademy.api.entity.Student;
import com.hiacademy.api.entity.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findAllByParent_Id(Long parentId);
    List<Student> findAllByClassroom_Id(Long classroomId);

    List<Student> findAllByClassroom_IdIn(Collection<Long> classroomIds);

    List<Student> findAllByAcademy_IdOrderByCreatedAtDesc(Long academyId);

    @Query("""
        SELECT DISTINCT s FROM Student s
        LEFT JOIN FETCH s.classroom c
        LEFT JOIN s.parent p
        WHERE s.academy.id = :academyId
           OR (s.academy IS NULL AND p.academy.id = :academyId)
        ORDER BY s.createdAt DESC
        """)
    List<Student> findAllByAcademyIdWithClassroom(@Param("academyId") Long academyId);

    @Query("SELECT s FROM Student s JOIN s.parent p WHERE p.academy.id = :academyId")
    List<Student> findAllByAcademyIdLegacy(@Param("academyId") Long academyId);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.academy.id = :academyId AND s.status <> :withdrawn")
    long countByAcademyIdExcludingWithdrawn(@Param("academyId") Long academyId, @Param("withdrawn") StudentStatus withdrawn);

    @Query("""
        SELECT COUNT(s) FROM Student s
        WHERE COALESCE(s.academy.id, s.parent.academy.id) = :academyId AND s.status <> :withdrawn
        """)
    long countByAcademyIdExcludingWithdrawnLegacy(@Param("academyId") Long academyId, @Param("withdrawn") StudentStatus withdrawn);

    List<Student> findAllByLoginPhone(String loginPhone);

    List<Student> findAllByAcademy_IdAndLoginPhone(Long academyId, String loginPhone);

    Optional<Student> findByIdAndAcademy_Id(Long id, Long academyId);

    @Query("""
        SELECT s FROM Student s
        LEFT JOIN FETCH s.classroom
        WHERE s.academy.id = :academyId AND s.loginPhone = :loginPhone
        """)
    List<Student> findSiblingsByAcademyAndLoginPhone(@Param("academyId") Long academyId, @Param("loginPhone") String loginPhone);
}
