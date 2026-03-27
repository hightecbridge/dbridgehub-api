package com.hiacademy.api.service;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.entity.*;

class Mapper {
    static AcademyInfo toAcademyInfo(Academy a) {
        if (a==null) return null;
        return AcademyInfo.builder().id(a.getId()).name(a.getName())
            .address(a.getAddress()).desc(a.getDescription())
            .phone(a.getPhone()).logoBase64(a.getLogoBase64()).build();
    }
    static ClassRoomResponse toClassRoom(ClassRoom c) {
        return ClassRoomResponse.builder().id(c.getId()).name(c.getName())
            .subject(c.getSubject()).teacher(c.getTeacher()).schedule(c.getSchedule())
            .capacity(c.getCapacity()).tuitionFee(c.getTuitionFee()).bookFee(c.getBookFee())
            .color(c.getColor()).textColor(c.getTextColor()).createdAt(c.getCreatedAt()).build();
    }
    static FeeResponse toFee(FeeRecord f) {
        return FeeResponse.builder().id(f.getId()).label(f.getLabel())
            .amount(f.getAmount()).paid(f.isPaid()).yearMonth(f.getYearMonth()).build();
    }
    static StudentResponse toStudent(Student s) {
        return StudentResponse.builder().id(s.getId()).name(s.getName()).grade(s.getGrade())
            .birthDate(s.getBirthDate()).status(s.getStatus()!=null?s.getStatus().name():null)
            .classroomId(s.getClassroom()!=null?s.getClassroom().getId():null)
            .classroomName(s.getClassroom()!=null?s.getClassroom().getName():null)
            .fees(s.getFees().stream().map(Mapper::toFee).toList()).build();
    }
    static ParentResponse toParent(Parent p) {
        return ParentResponse.builder().id(p.getId()).name(p.getName()).phone(p.getPhone())
            .badgeColor(p.getBadgeColor()).badgeTextColor(p.getBadgeTextColor())
            .kakaoLinked(p.isKakaoLinked()).createdAt(p.getCreatedAt())
            .students(p.getStudents().stream().map(Mapper::toStudent).toList()).build();
    }
    static AttendRecordResponse toAttendRecord(AttendRecord r) {
        return AttendRecordResponse.builder().id(r.getId())
            .studentId(r.getStudent().getId()).studentName(r.getStudent().getName())
            .status(r.getStatus().name()).note(r.getNote())
            .date(r.getSheet().getAttendDate().toString()).build();
    }
    static AttendSheetResponse toAttendSheet(AttendSheet s) {
        return AttendSheetResponse.builder().id(s.getId())
            .classroomId(s.getClassroom().getId()).classroomName(s.getClassroom().getName())
            .date(s.getAttendDate().toString())
            .records(s.getRecords().stream().map(Mapper::toAttendRecord).toList())
            .createdAt(s.getCreatedAt()).build();
    }
    static HomeworkRecordResponse toHwRecord(HomeworkRecord r) {
        return HomeworkRecordResponse.builder().id(r.getId())
            .studentId(r.getStudent().getId()).studentName(r.getStudent().getName())
            .done(r.isDone()).comment(r.getComment())
            .date(r.getSheet().getHomeworkDate().toString()).title(r.getSheet().getTitle())
            .updatedAt(r.getUpdatedAt()).build();
    }
    static HomeworkSheetResponse toHwSheet(HomeworkSheet s) {
        return HomeworkSheetResponse.builder().id(s.getId())
            .classroomId(s.getClassroom().getId()).classroomName(s.getClassroom().getName())
            .date(s.getHomeworkDate().toString()).title(s.getTitle())
            .records(s.getRecords().stream().map(Mapper::toHwRecord).toList())
            .createdAt(s.getCreatedAt()).build();
    }
    static NoticeResponse toNotice(NoticeItem n) {
        String image = n.getImageData() != null ? n.getImageData() : n.getImageUrl();
        // targets는 (환경에 따라) lazy proxy일 수 있어, 서비스 트랜잭션 종료 후
        // JSON 직렬화 단계에서 "no Session" 예외가 날 수 있다.
        java.util.List<String> targets = n.getTargets() == null
            ? java.util.List.of("전체")
            : new java.util.ArrayList<>(n.getTargets());
        return NoticeResponse.builder().id(n.getId()).title(n.getTitle()).body(n.getBody())
            .targets(targets).imageUrl(image)
            .date(n.getDate()).createdAt(n.getCreatedAt()).build();
    }
    static EventResponse toEvent(CalendarEvent e) {
        java.util.List<String> targets = e.getTargets() == null
            ? java.util.List.of("전체")
            : new java.util.ArrayList<>(e.getTargets());
        return EventResponse.builder().id(e.getId()).title(e.getTitle())
            .date(e.getDate().toString())
            .endDate(e.getEndDate()!=null?e.getEndDate().toString():null)
            .category(e.getCategory()!=null?e.getCategory().name():null)
            .targets(targets).description(e.getDescription())
            .color(e.getColor()).allDay(e.isAllDay()).createdAt(e.getCreatedAt()).build();
    }
    static ConsultResponse toConsult(Consultation c) {
        return ConsultResponse.builder().id(c.getId())
            .studentId(c.getStudent().getId()).studentName(c.getStudent().getName())
            .date(c.getConsultDate().toString()).time(c.getConsultTime())
            .status(c.getStatus()!=null?c.getStatus().name():null)
            .content(c.getContent()).createdAt(c.getCreatedAt()).build();
    }
}
