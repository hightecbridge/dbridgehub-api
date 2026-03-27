package com.hiacademy.api.service;
import com.hiacademy.api.dto.request.AttendSheetRequest;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate; import java.util.List;

@Service @Transactional
public class AttendService {
    private final AttendSheetRepository  sheetRepo;
    private final AttendRecordRepository recordRepo;
    private final ClassRoomRepository    clsRepo;
    private final StudentRepository      stuRepo;
    public AttendService(AttendSheetRepository s, AttendRecordRepository r, ClassRoomRepository c, StudentRepository st) {
        sheetRepo=s; recordRepo=r; clsRepo=c; stuRepo=st;
    }
    @Transactional(readOnly=true)
    public List<AttendSheetResponse> listSheets(Long academyId, Long classroomId) {
        return sheetRepo.findAllByClassroom_IdOrderByAttendDateDesc(classroomId).stream().map(Mapper::toAttendSheet).toList();
    }
    public AttendSheetResponse saveSheet(Long academyId, Long classroomId, AttendSheetRequest req) {
        ClassRoom cls = clsRepo.findByIdAndAcademy_Id(classroomId,academyId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        LocalDate date = LocalDate.parse(req.getDate());
        AttendSheet sheet = sheetRepo.findByClassroom_IdAndAttendDate(classroomId,date)
            .orElseGet(()->sheetRepo.save(AttendSheet.builder().classroom(cls).attendDate(date).build()));
        for (var item : req.getRecords()) {
            Student stu = stuRepo.findById(item.getStudentId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
            AttendRecord rec = recordRepo.findBySheet_IdAndStudent_Id(sheet.getId(),stu.getId())
                .orElseGet(()->AttendRecord.builder().sheet(sheet).student(stu).build());
            rec.setStatus(AttendStatus.valueOf(item.getStatus())); rec.setNote(item.getNote());
            recordRepo.save(rec);
        }
        return Mapper.toAttendSheet(sheetRepo.findById(sheet.getId()).orElseThrow());
    }
    public void deleteSheet(Long academyId, Long classroomId, LocalDate date) {
        clsRepo.findByIdAndAcademy_Id(classroomId, academyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        AttendSheet sheet = sheetRepo.findByClassroom_IdAndAttendDate(classroomId, date)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        sheetRepo.delete(sheet);
    }
    @Transactional(readOnly=true)
    public List<AttendRecordResponse> getStudentAttend(Long studentId) {
        return recordRepo.findAllByStudentIdOrderByDateDesc(studentId).stream().map(Mapper::toAttendRecord).toList();
    }
}
