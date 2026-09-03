package com.hiacademy.api.repository;

import com.hiacademy.api.entity.FeeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {
    List<FeeRecord> findAllByStudent_Id(Long studentId);
    List<FeeRecord> findAllByStudent_IdAndYearMonth(Long studentId, int yearMonth);
    void deleteAllByStudent_Id(Long studentId);

    @Query("""
        SELECT f FROM FeeRecord f
        JOIN FETCH f.student s
        LEFT JOIN s.parent p
        WHERE COALESCE(s.academy.id, p.academy.id) = :academyId
          AND f.yearMonth BETWEEN :fromYm AND :toYm
        """)
    List<FeeRecord> findAllByAcademyIdAndYearMonthBetween(
        @Param("academyId") Long academyId,
        @Param("fromYm") int fromYm,
        @Param("toYm") int toYm);
}
