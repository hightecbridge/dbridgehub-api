package com.hiacademy.api.service;
import com.hiacademy.api.dto.request.ConsultRequest;
import com.hiacademy.api.dto.response.ConsultResponse;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate; import java.util.List;

@Service @Transactional
public class ConsultationService {
    private final ConsultationRepository consultRepo;
    private final StudentRepository      stuRepo;
    public ConsultationService(ConsultationRepository c, StudentRepository s) { consultRepo=c; stuRepo=s; }
    @Transactional(readOnly=true)
    public List<ConsultResponse> listByAcademy(Long academyId) {
        return consultRepo.findAllByAcademyId(academyId).stream().map(Mapper::toConsult).toList();
    }
    @Transactional(readOnly=true)
    public List<ConsultResponse> listByParent(Long parentId) {
        return consultRepo.findAllByParentId(parentId).stream().map(Mapper::toConsult).toList();
    }
    public ConsultResponse request(Long parentId, ConsultRequest req) {
        Student stu = stuRepo.findById(req.getStudentId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!stu.getParent().getId().equals(parentId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return Mapper.toConsult(consultRepo.save(Consultation.builder()
            .student(stu).consultDate(LocalDate.parse(req.getDate()))
            .consultTime(req.getTime()).content(req.getContent()).status(ConsultStatus.대기).build()));
    }
    public ConsultResponse updateStatus(Long academyId, Long id, String status) {
        Consultation c = consultRepo.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!c.getStudent().getParent().getAcademy().getId().equals(academyId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        c.setStatus(ConsultStatus.valueOf(status));
        return Mapper.toConsult(consultRepo.save(c));
    }
}
