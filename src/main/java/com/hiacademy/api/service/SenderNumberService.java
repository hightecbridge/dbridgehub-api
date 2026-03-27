package com.hiacademy.api.service;

import com.hiacademy.api.dto.request.SenderNumberRequest;
import com.hiacademy.api.dto.response.SenderNumberResponse;
import com.hiacademy.api.entity.Academy;
import com.hiacademy.api.entity.SenderNumberItem;
import com.hiacademy.api.repository.AcademyRepository;
import com.hiacademy.api.repository.SenderNumberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SenderNumberService {
    private final SenderNumberRepository repo;
    private final AcademyRepository academyRepo;

    public SenderNumberService(SenderNumberRepository repo, AcademyRepository academyRepo) {
        this.repo = repo;
        this.academyRepo = academyRepo;
    }

    @Transactional
    public List<SenderNumberResponse> list(Long academyId) {
        return repo.findAllByAcademy_IdOrderByCreatedAtDesc(academyId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public SenderNumberResponse create(Long academyId, SenderNumberRequest req) {
        Academy a = academyRepo.getReferenceById(academyId);
        SenderNumberItem saved = repo.save(SenderNumberItem.builder()
            .academy(a)
            .label(req.getLabel())
            .number(req.getNumber())
            .isDefault(false)
            .build());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long academyId, Long id) {
        // 기본 항목 삭제 시에도 일단 삭제만 진행(프론트에서 기본 선택 로직이 보완됨)
        repo.deleteByIdAndAcademy_Id(id, academyId);
    }

    @Transactional
    public SenderNumberResponse setDefault(Long academyId, Long id) {
        SenderNumberItem target = repo.findByIdAndAcademy_Id(id, academyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "발신번호를 찾을 수 없습니다."));

        // 기존 default 해제
        repo.findAllByAcademy_IdOrderByCreatedAtDesc(academyId)
            .forEach(n -> n.setDefault(false));

        target.setDefault(true);
        repo.save(target);
        return toResponse(target);
    }

    private SenderNumberResponse toResponse(SenderNumberItem n) {
        if (n == null) return null;
        return SenderNumberResponse.builder()
            .id(n.getId())
            .label(n.getLabel())
            .number(n.getNumber())
            .isDefault(n.isDefault())
            .build();
    }
}

