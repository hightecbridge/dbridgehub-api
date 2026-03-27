package com.hiacademy.api.repository;

import com.hiacademy.api.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    @Query("SELECT c FROM Consultation c JOIN c.student s JOIN s.parent p " +
           "WHERE p.academy.id = :academyId ORDER BY c.consultDate DESC")
    List<Consultation> findAllByAcademyId(@Param("academyId") Long academyId);

    @Query("SELECT c FROM Consultation c WHERE c.student.parent.id = :parentId " +
           "ORDER BY c.consultDate DESC")
    List<Consultation> findAllByParentId(@Param("parentId") Long parentId);
}
