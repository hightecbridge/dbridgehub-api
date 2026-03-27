package com.hiacademy.api.service;
import com.hiacademy.api.dto.request.*;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate; import java.util.List;

@Service @Transactional
public class HomeworkService {
    private final HomeworkSheetRepository  sheetRepo;
    private final HomeworkRecordRepository recordRepo;
    private final ClassRoomRepository      clsRepo;
    private final StudentRepository        stuRepo;
    public HomeworkService(HomeworkSheetRepository s, HomeworkRecordRepository r, ClassRoomRepository c, StudentRepository st) {
        sheetRepo=s; recordRepo=r; clsRepo=c; stuRepo=st;
    }
    @Transactional(readOnly=true)
    public List<HomeworkSheetResponse> listSheets(Long academyId, Long classroomId) {
        return sheetRepo.findAllByClassroom_IdOrderByHomeworkDateDesc(classroomId).stream().map(Mapper::toHwSheet).toList();
    }
    public HomeworkSheetResponse saveSheet(Long academyId, Long classroomId, HomeworkSheetRequest req) {
        ClassRoom cls = clsRepo.findByIdAndAcademy_Id(classroomId,academyId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        LocalDate date = LocalDate.parse(req.getDate());
        HomeworkSheet sheet = sheetRepo.findByClassroom_IdAndHomeworkDate(classroomId,date)
            .orElseGet(()->HomeworkSheet.builder().classroom(cls).homeworkDate(date).title(req.getTitle()).build());
        sheet.setTitle(req.getTitle()); sheetRepo.save(sheet);
        for (var item : req.getRecords()) {
            Student stu = stuRepo.findById(item.getStudentId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
            HomeworkRecord rec = recordRepo.findBySheet_IdAndStudent_Id(sheet.getId(),stu.getId())
                .orElseGet(()->HomeworkRecord.builder().sheet(sheet).student(stu).build());
            rec.setDone(item.isDone()); rec.setComment(item.getComment()); recordRepo.save(rec);
        }
        return Mapper.toHwSheet(sheetRepo.findById(sheet.getId()).orElseThrow());
    }
    public HomeworkRecordResponse updateRecord(Long sheetId, Long studentId, HomeworkRecordUpdate req) {
        HomeworkRecord rec = recordRepo.findBySheet_IdAndStudent_Id(sheetId,studentId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        rec.setDone(req.isDone()); rec.setComment(req.getComment());
        return Mapper.toHwRecord(recordRepo.save(rec));
    }
    public void deleteSheet(Long academyId, Long classroomId, Long sheetId) {
        HomeworkSheet sheet = sheetRepo.findById(sheetId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!sheet.getClassroom().getId().equals(classroomId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!sheet.getClassroom().getAcademy().getId().equals(academyId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        sheetRepo.delete(sheet);
    }
    @Transactional(readOnly=true)
    public List<HomeworkRecordResponse> getStudentHomework(Long studentId) {
        return recordRepo.findAllByStudentIdOrderByDateDesc(studentId).stream().map(Mapper::toHwRecord).toList();
    }
}
