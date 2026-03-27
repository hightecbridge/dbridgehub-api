package com.hiacademy.api.service;
import com.hiacademy.api.dto.request.ClassRoomRequest;
import com.hiacademy.api.dto.response.ClassRoomResponse;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service @Transactional
public class ClassRoomService {
    private final ClassRoomRepository clsRepo;
    private final AcademyRepository   academyRepo;
    public ClassRoomService(ClassRoomRepository c, AcademyRepository a) { clsRepo=c; academyRepo=a; }
    @Transactional(readOnly=true)
    public List<ClassRoomResponse> list(Long academyId) {
        return clsRepo.findAllByAcademy_IdOrderByCreatedAtAsc(academyId).stream().map(Mapper::toClassRoom).toList();
    }
    public ClassRoomResponse create(Long academyId, ClassRoomRequest req) {
        Academy a = academyRepo.getReferenceById(academyId);
        return Mapper.toClassRoom(clsRepo.save(ClassRoom.builder()
            .name(req.getName()).subject(req.getSubject()).teacher(req.getTeacher())
            .schedule(req.getSchedule()).capacity(req.getCapacity())
            .tuitionFee(req.getTuitionFee()).bookFee(req.getBookFee())
            .color(req.getColor()).textColor(req.getTextColor()).academy(a).build()));
    }
    public ClassRoomResponse update(Long academyId, Long id, ClassRoomRequest req) {
        ClassRoom c = clsRepo.findByIdAndAcademy_Id(id,academyId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.setName(req.getName()); c.setSubject(req.getSubject()); c.setTeacher(req.getTeacher());
        c.setSchedule(req.getSchedule()); c.setCapacity(req.getCapacity());
        c.setTuitionFee(req.getTuitionFee()); c.setBookFee(req.getBookFee());
        c.setColor(req.getColor()); c.setTextColor(req.getTextColor());
        return Mapper.toClassRoom(clsRepo.save(c));
    }
    public void delete(Long academyId, Long id) {
        clsRepo.delete(clsRepo.findByIdAndAcademy_Id(id,academyId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }
}
