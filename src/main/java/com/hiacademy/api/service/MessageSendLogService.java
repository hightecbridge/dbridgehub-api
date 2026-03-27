package com.hiacademy.api.service;

import com.hiacademy.api.dto.request.MessageSendLogRequest;
import com.hiacademy.api.dto.response.MessageSendLogResponse;
import com.hiacademy.api.entity.Academy;
import com.hiacademy.api.entity.MessageSendKind;
import com.hiacademy.api.entity.MessageSendLog;
import com.hiacademy.api.repository.AcademyRepository;
import com.hiacademy.api.repository.MessageSendLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MessageSendLogService {
    private final MessageSendLogRepository repo;
    private final AcademyRepository academyRepo;

    public MessageSendLogService(MessageSendLogRepository repo, AcademyRepository academyRepo) {
        this.repo = repo;
        this.academyRepo = academyRepo;
    }

    @Transactional(readOnly = true)
    public List<MessageSendLogResponse> list(Long academyId) {
        return repo.findTop50ByAcademy_IdOrderByCreatedAtDesc(academyId).stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public MessageSendLogResponse create(Long academyId, MessageSendLogRequest req) {
        Academy a = academyRepo.getReferenceById(academyId);
        MessageSendKind kind;
        try {
            kind = MessageSendKind.valueOf(req.getKind().trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kind는 CLASS, ALL, PAYMENT 중 하나여야 합니다.");
        }
        MessageSendLog saved = repo.save(MessageSendLog.builder()
            .academy(a)
            .kind(kind)
            .targetLabel(req.getTargetLabel())
            .title(req.getTitle())
            .bodyPreview(req.getBodyPreview())
            .recipientCount(req.getRecipientCount())
            .build());
        return toResponse(saved);
    }

    private MessageSendLogResponse toResponse(MessageSendLog e) {
        return MessageSendLogResponse.builder()
            .id(e.getId())
            .kind(e.getKind().name())
            .targetLabel(e.getTargetLabel())
            .title(e.getTitle())
            .bodyPreview(e.getBodyPreview())
            .recipientCount(e.getRecipientCount())
            .createdAt(e.getCreatedAt())
            .build();
    }
}
