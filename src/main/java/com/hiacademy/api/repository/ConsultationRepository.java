package com.hiacademy.api.repository;

import com.hiacademy.api.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    @Query("""
        SELECT c FROM Consultation c
        LEFT JOIN c.student s
        LEFT JOIN s.parent p
        WHERE c.academy.id = :academyId
           OR COALESCE(s.academy.id, p.academy.id) = :academyId
        ORDER BY c.consultDate DESC, c.id DESC
        """)
    List<Consultation> findAllByAcademyId(@Param("academyId") Long academyId);

    @Query("""
        SELECT c FROM Consultation c
        LEFT JOIN c.student s
        LEFT JOIN s.parent p
        WHERE (c.academy.id = :academyId OR COALESCE(s.academy.id, p.academy.id) = :academyId)
          AND c.consultDate BETWEEN :from AND :to
        ORDER BY c.consultDate ASC, c.id ASC
        """)
    List<Consultation> findAllByAcademyIdAndDateBetween(
        @Param("academyId") Long academyId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);

    @Query("""
        SELECT c FROM Consultation c JOIN c.student s
        WHERE s.academy.id = :academyId AND s.loginPhone = :loginPhone
        ORDER BY c.consultDate DESC
        """)
    List<Consultation> findAllByAcademyIdAndLoginPhone(
        @Param("academyId") Long academyId,
        @Param("loginPhone") String loginPhone);

    List<Consultation> findAllByStudent_IdOrderByConsultDateDescIdDesc(Long studentId);

    Optional<Consultation> findByIdAndAcademy_Id(Long id, Long academyId);

    void deleteAllByStudent_Id(Long studentId);
}
