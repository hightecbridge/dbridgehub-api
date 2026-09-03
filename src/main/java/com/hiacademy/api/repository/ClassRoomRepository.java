package com.hiacademy.api.repository;

import com.hiacademy.api.entity.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {
    List<ClassRoom> findAllByAcademy_IdOrderByCreatedAtAsc(Long academyId);

    @Query("SELECT DISTINCT c FROM ClassRoom c LEFT JOIN FETCH c.teacherUser WHERE c.academy.id = :academyId ORDER BY c.createdAt ASC")
    List<ClassRoom> findAllByAcademyIdWithTeacher(@Param("academyId") Long academyId);

    List<ClassRoom> findAllByTeacherUser_IdAndAcademy_Id(Long teacherUserId, Long academyId);
    Optional<ClassRoom> findByIdAndAcademy_Id(Long id, Long academyId);
}
