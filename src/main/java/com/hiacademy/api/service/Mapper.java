package com.hiacademy.api.service;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.menu.MenuSettings;
import java.time.LocalDate;
import java.util.List;

class Mapper {

    static int currentYearMonth() {
        LocalDate now = LocalDate.now();
        return now.getYear() * 100 + now.getMonthValue();
    }

    static int normalizeYearMonth(Integer yearMonth) {
        if (yearMonth == null || yearMonth < 100001) {
            return currentYearMonth();
        }
        int month = yearMonth % 100;
        if (month < 1 || month > 12) {
            return currentYearMonth();
        }
        return yearMonth;
    }
    static AcademyInfo toAcademyInfo(Academy a) {
        if (a==null) return null;
        return AcademyInfo.builder().id(a.getId()).name(a.getName())
            .address(a.getAddress()).desc(a.getDescription())
            .phone(a.getPhone()).logoBase64(a.getLogoBase64())
            .menuSettings(MenuSettings.merge(a.getMenuSettingsJson()))
            .build();
    }
    static ClassRoomResponse toClassRoom(ClassRoom c) {
        return ClassRoomResponse.builder().id(c.getId()).name(c.getName())
            .subject(c.getSubject()).teacher(c.getTeacher())
            .teacherUserId(c.getTeacherUser() != null ? c.getTeacherUser().getId() : null)
            .schedule(c.getSchedule())
            .capacity(c.getCapacity()).tuitionFee(c.getTuitionFee()).bookFee(c.getBookFee())
            .color(c.getColor()).textColor(c.getTextColor()).createdAt(c.getCreatedAt()).build();
    }
    static FeeResponse toFee(FeeRecord f) {
        return FeeResponse.builder().id(f.getId()).label(f.getLabel())
            .amount(f.getAmount()).paid(f.isPaid()).yearMonth(f.getYearMonth())
            .paidAt(f.getPaidAt() != null ? f.getPaidAt().toString() : null)
            .paymentMethod(f.getPaymentMethod())
            .build();
    }
    static StudentResponse toStudent(Student s) {
        return toStudent(s, currentYearMonth());
    }

    static StudentResponse toStudent(Student s, int yearMonth) {
        int ym = normalizeYearMonth(yearMonth);
        List<FeeResponse> fees = s.getFees() == null ? List.of() : s.getFees().stream()
            .filter(f -> f.getYearMonth() == ym)
            .map(Mapper::toFee)
            .toList();
        return StudentResponse.builder().id(s.getId()).name(s.getName()).grade(s.getGrade())
            .phone(s.getPhone())
            .parentName(s.resolveParentName())
            .parentPhone(s.resolveParentPhone())
            .badgeColor(s.getBadgeColor() != null ? s.getBadgeColor()
                : (s.getParent() != null ? s.getParent().getBadgeColor() : null))
            .badgeTextColor(s.getBadgeTextColor() != null ? s.getBadgeTextColor()
                : (s.getParent() != null ? s.getParent().getBadgeTextColor() : null))
            .kakaoLinked(s.isKakaoLinked() || (s.getParent() != null && s.getParent().isKakaoLinked()))
            .createdAt(s.getCreatedAt() != null ? s.getCreatedAt()
                : (s.getParent() != null ? s.getParent().getCreatedAt() : null))
            .birthDate(s.getBirthDate()).status(s.getStatus()!=null?s.getStatus().name():null)
            .classroomId(s.getClassroom()!=null?s.getClassroom().getId():null)
            .classroomName(s.getClassroom()!=null?s.getClassroom().getName():null)
            .withdrawnAt(s.getWithdrawnAt())
            .fees(fees).build();
    }

    static ParentResponse toParent(Parent p) {
        return toParent(p, currentYearMonth());
    }

    static ParentResponse toParent(Parent p, int yearMonth) {
        int ym = normalizeYearMonth(yearMonth);
        return ParentResponse.builder().id(p.getId()).name(p.getName()).phone(p.getPhone())
            .badgeColor(p.getBadgeColor()).badgeTextColor(p.getBadgeTextColor())
            .kakaoLinked(p.isKakaoLinked()).createdAt(p.getCreatedAt())
            .students(p.getStudents().stream().map(st -> toStudent(st, ym)).toList()).build();
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
        java.util.List<String> targets = n.getTargets() == null
            ? java.util.List.of("전체")
            : new java.util.ArrayList<>(n.getTargets());
        java.util.List<NoticeAttachmentResponse> files = n.getAttachments() == null
            ? java.util.List.of()
            : n.getAttachments().stream()
                .map(a -> NoticeAttachmentResponse.builder()
                    .id(a.getId())
                    .fileName(a.getFileName())
                    .contentType(a.getContentType())
                    .sizeBytes(a.getSizeBytes())
                    .data(a.getData())
                    .sortOrder(a.getSortOrder())
                    .build())
                .toList();
        // 하위호환: files 중 첫 이미지가 있으면 imageUrl로도 노출
        if ((image == null || image.isBlank()) && !files.isEmpty()) {
            NoticeAttachmentResponse firstImage = files.stream()
                .filter(f -> f.getContentType() != null && f.getContentType().startsWith("image/"))
                .findFirst()
                .orElse(files.get(0));
            if (firstImage.getData() != null && (
                firstImage.getContentType() == null
                    || firstImage.getContentType().startsWith("image/")
                    || firstImage.getData().startsWith("data:image/")
            )) {
                image = firstImage.getData();
            }
        }
        return NoticeResponse.builder().id(n.getId()).title(n.getTitle()).body(n.getBody())
            .targets(targets).imageUrl(image).files(files)
            .date(n.getDate()).createdAt(n.getCreatedAt()).build();
    }
    static NoticeResponse toClassNotice(ClassNotice n) {
        String image = n.getImageData() != null ? n.getImageData() : n.getImageUrl();
        java.util.List<String> targets = n.getTargets() == null
            ? java.util.List.of("전체")
            : new java.util.ArrayList<>(n.getTargets());
        java.util.List<NoticeAttachmentResponse> files = n.getAttachments() == null
            ? java.util.List.of()
            : n.getAttachments().stream()
                .map(a -> NoticeAttachmentResponse.builder()
                    .id(a.getId())
                    .fileName(a.getFileName())
                    .contentType(a.getContentType())
                    .sizeBytes(a.getSizeBytes())
                    .data(a.getData())
                    .sortOrder(a.getSortOrder())
                    .build())
                .toList();
        if ((image == null || image.isBlank()) && !files.isEmpty()) {
            NoticeAttachmentResponse firstImage = files.stream()
                .filter(f -> f.getContentType() != null && f.getContentType().startsWith("image/"))
                .findFirst()
                .orElse(files.get(0));
            if (firstImage.getData() != null && (
                firstImage.getContentType() == null
                    || firstImage.getContentType().startsWith("image/")
                    || firstImage.getData().startsWith("data:image/")
            )) {
                image = firstImage.getData();
            }
        }
        return NoticeResponse.builder().id(n.getId()).title(n.getTitle()).body(n.getBody())
            .targets(targets).imageUrl(image).files(files)
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
        Student s = c.getStudent();
        User t = c.getTeacher();
        String kind = c.getKind() != null ? c.getKind().name() : ConsultKind.재원생.name();
        return ConsultResponse.builder()
            .id(c.getId())
            .kind(kind)
            .studentId(s != null ? s.getId() : null)
            .studentName(c.displayName())
            .studentStatus(s != null && s.getStatus() != null ? s.getStatus().name() : null)
            .classroomName(s != null && s.getClassroom() != null ? s.getClassroom().getName() : null)
            .teacherUserId(t != null ? t.getId() : null)
            .teacherName(t != null ? t.getName() : null)
            .date(c.getConsultDate().toString())
            .time(c.getConsultTime())
            .status(c.getStatus() != null ? c.getStatus().name() : null)
            .content(c.getContent())
            .prospectName(c.getProspectName())
            .prospectPhone(c.getProspectPhone())
            .prospectGrade(c.getProspectGrade())
            .prospectParentName(c.getProspectParentName())
            .createdAt(c.getCreatedAt())
            .build();
    }
}
