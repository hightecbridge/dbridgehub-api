package com.hiacademy.api.service;
import com.hiacademy.api.dto.request.CalendarEventRequest;
import com.hiacademy.api.dto.response.EventResponse;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate; import java.util.List;
import org.hibernate.Hibernate;

@Service @Transactional
public class CalendarService {
    private final CalendarEventRepository eventRepo;
    private final AcademyRepository       academyRepo;
    public CalendarService(CalendarEventRepository e, AcademyRepository a) { eventRepo=e; academyRepo=a; }
    @Transactional(readOnly=true)
    public List<EventResponse> list(Long academyId) {
        var items = eventRepo.findAllByAcademy_IdOrderByDateAsc(academyId);
        // targets가 lazy proxy인 경우 JSON 직렬화 직전에 강제 초기화
        items.forEach(e -> Hibernate.initialize(e.getTargets()));
        return items.stream().map(Mapper::toEvent).toList();
    }
    public EventResponse create(Long academyId, CalendarEventRequest req) {
        Academy a = academyRepo.getReferenceById(academyId);
        return Mapper.toEvent(eventRepo.save(CalendarEvent.builder()
            .title(req.getTitle()).date(LocalDate.parse(req.getDate()))
            .endDate(req.getEndDate()!=null?LocalDate.parse(req.getEndDate()):null)
            .category(req.getCategory()!=null?EventCategory.valueOf(req.getCategory()):null)
            .targets(req.getTargets()).description(req.getDescription())
            .color(req.getColor()).allDay(req.isAllDay()).academy(a).build()));
    }
    public EventResponse update(Long academyId, Long id, CalendarEventRequest req) {
        CalendarEvent e = eventRepo.findById(id).filter(ev->ev.getAcademy().getId().equals(academyId))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        e.setTitle(req.getTitle()); e.setDate(LocalDate.parse(req.getDate()));
        e.setEndDate(req.getEndDate()!=null?LocalDate.parse(req.getEndDate()):null);
        e.setCategory(req.getCategory()!=null?EventCategory.valueOf(req.getCategory()):null);
        e.setTargets(req.getTargets()); e.setDescription(req.getDescription());
        e.setColor(req.getColor()); e.setAllDay(req.isAllDay());
        return Mapper.toEvent(eventRepo.save(e));
    }
    public void delete(Long academyId, Long id) {
        CalendarEvent e = eventRepo.findById(id).filter(ev->ev.getAcademy().getId().equals(academyId))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        eventRepo.delete(e);
    }
}
