package com.hiacademy.api.repository;

import com.hiacademy.api.entity.HomeworkSheet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HomeworkSheetRepository extends JpaRepository<HomeworkSheet, Long> {
    List<HomeworkSheet> findAllByClassroom_IdOrderByHomeworkDateDesc(Long classroomId);
    Optional<HomeworkSheet> findByClassroom_IdAndHomeworkDate(Long classroomId, LocalDate date);
}
