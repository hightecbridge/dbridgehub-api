package com.hiacademy.api.repository;

import com.hiacademy.api.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    List<CalendarEvent> findAllByAcademy_IdOrderByDateAsc(Long academyId);

    @Query("SELECT e FROM CalendarEvent e WHERE e.academy.id = :academyId " +
           "AND e.date BETWEEN :from AND :to ORDER BY e.date ASC")
    List<CalendarEvent> findByAcademyAndDateRange(
        @Param("academyId") Long academyId,
        @Param("from") LocalDate from,
        @Param("to")   LocalDate to
    );
}
