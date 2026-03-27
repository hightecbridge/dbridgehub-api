package com.hiacademy.api.repository;

import com.hiacademy.api.entity.AttendRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendRecordRepository extends JpaRepository<AttendRecord, Long> {
    List<AttendRecord> findAllBySheet_Id(Long sheetId);
    Optional<AttendRecord> findBySheet_IdAndStudent_Id(Long sheetId, Long studentId);

    @Query("SELECT r FROM AttendRecord r JOIN r.sheet s " +
           "WHERE r.student.id = :studentId ORDER BY s.attendDate DESC")
    List<AttendRecord> findAllByStudentIdOrderByDateDesc(@Param("studentId") Long studentId);
}
