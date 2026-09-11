package com.hiacademy.api.service;

import com.hiacademy.api.attend.KoreanHolidays;
import com.hiacademy.api.dto.response.AttendSettingsResponse;
import com.hiacademy.api.entity.Academy;
import com.hiacademy.api.entity.CalendarEvent;
import com.hiacademy.api.entity.EventCategory;
import com.hiacademy.api.repository.AcademyRepository;
import com.hiacademy.api.repository.CalendarEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class AttendHolidayService {
    private final AcademyRepository academyRepo;
    private final CalendarEventRepository eventRepo;

    public AttendHolidayService(AcademyRepository academyRepo, CalendarEventRepository eventRepo) {
        this.academyRepo = academyRepo;
        this.eventRepo = eventRepo;
    }

    @Transactional(readOnly = true)
    public AttendSettingsResponse get(Long academyId) {
        return AttendSettingsResponse.builder()
            .excludeHolidaysFromAttend(excludeHolidays(academyId))
            .build();
    }

    public AttendSettingsResponse save(Long academyId, Boolean excludeHolidaysFromAttend) {
        Academy academy = academy(academyId);
        academy.setExcludeHolidaysFromAttend(Boolean.TRUE.equals(excludeHolidaysFromAttend));
        academyRepo.save(academy);
        return AttendSettingsResponse.builder()
            .excludeHolidaysFromAttend(Boolean.TRUE.equals(academy.getExcludeHolidaysFromAttend()))
            .build();
    }

    @Transactional(readOnly = true)
    public boolean excludeHolidays(Long academyId) {
        return Boolean.TRUE.equals(academy(academyId).getExcludeHolidaysFromAttend());
    }

    /** 설정이 켜져 있고 해당일이 공휴일/학원 휴일이면 출석 스킵. */
    @Transactional(readOnly = true)
    public String skipReason(Long academyId, String className, LocalDate date) {
        if (!excludeHolidays(academyId)) return null;
        return holidayName(academyId, className, date);
    }

    @Transactional(readOnly = true)
    public String holidayName(Long academyId, String className, LocalDate date) {
        String kr = KoreanHolidays.nameOf(date);
        if (kr != null) return kr;
        List<CalendarEvent> events = eventRepo.findAllByAcademy_IdAndCategory(academyId, EventCategory.휴일);
        for (CalendarEvent ev : events) {
            if (!covers(ev, date)) continue;
            if (!targetsClass(ev, className)) continue;
            String title = ev.getTitle() == null || ev.getTitle().isBlank() ? "학원 휴일" : ev.getTitle();
            return title;
        }
        return null;
    }

    private static boolean covers(CalendarEvent ev, LocalDate date) {
        LocalDate start = ev.getDate();
        LocalDate end = ev.getEndDate() != null ? ev.getEndDate() : start;
        return date != null && !date.isBefore(start) && !date.isAfter(end);
    }

    private static boolean targetsClass(CalendarEvent ev, String className) {
        List<String> targets = ev.getTargets();
        if (targets == null || targets.isEmpty()) return true;
        if (targets.contains("전체")) return true;
        return className != null && targets.contains(className);
    }

    private Academy academy(Long academyId) {
        return academyRepo.findById(academyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학원 정보를 찾을 수 없습니다."));
    }
}
