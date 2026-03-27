package com.hiacademy.api.repository;

import com.hiacademy.api.entity.FeeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {
    List<FeeRecord> findAllByStudent_Id(Long studentId);
    List<FeeRecord> findAllByStudent_IdAndYearMonth(Long studentId, int yearMonth);
}
