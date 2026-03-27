package com.hiacademy.api.repository;

import com.hiacademy.api.entity.AttendSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendSheetRepository extends JpaRepository<AttendSheet, Long> {
    List<AttendSheet> findAllByClassroom_IdOrderByAttendDateDesc(Long classroomId);
    Optional<AttendSheet> findByClassroom_IdAndAttendDate(Long classroomId, LocalDate date);
}
