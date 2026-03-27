package com.hiacademy.api.repository;

import com.hiacademy.api.entity.HomeworkRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkRecordRepository extends JpaRepository<HomeworkRecord, Long> {
    List<HomeworkRecord> findAllBySheet_Id(Long sheetId);
    Optional<HomeworkRecord> findBySheet_IdAndStudent_Id(Long sheetId, Long studentId);

    @Query("SELECT r FROM HomeworkRecord r JOIN r.sheet s " +
           "WHERE r.student.id = :studentId ORDER BY s.homeworkDate DESC")
    List<HomeworkRecord> findAllByStudentIdOrderByDateDesc(@Param("studentId") Long studentId);
}
