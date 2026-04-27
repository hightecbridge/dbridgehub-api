package com.hiacademy.api.service;

import com.hiacademy.api.dto.request.MessageSendLogRequest;
import com.hiacademy.api.dto.response.MessageSendLogResponse;
import com.hiacademy.api.entity.Academy;
import com.hiacademy.api.entity.MessageGatewayProvider;
import com.hiacademy.api.entity.MessageSendKind;
import com.hiacademy.api.entity.MessageSendLog;
import com.hiacademy.api.repository.AcademyRepository;
import com.hiacademy.api.repository.MessageSendLogRepository;
import com.sms.aligo.dto.AligoSmsSendRequest;
import com.sms.aligo.service.AligoSmsService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MessageSendLogService {
    private final MessageSendLogRepository repo;
    private final AcademyRepository academyRepo;
    private final AligoSmsService aligoSmsService;
    private final AdminBillingService adminBillingService;

    public MessageSendLogService(
        MessageSendLogRepository repo,
        AcademyRepository academyRepo,
        AligoSmsService aligoSmsService,
        AdminBillingService adminBillingService
    ) {
        this.repo = repo;
        this.academyRepo = academyRepo;
        this.aligoSmsService = aligoSmsService;
        this.adminBillingService = adminBillingService;
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
        SendResult sendResult = sendIfRequested(academyId, req);

        MessageSendKind kind;
        try {
            kind = MessageSendKind.valueOf(req.getKind().trim().toUpperCase());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "kind는 CLASS, ALL, PAYMENT 중 하나여야 합니다.");
        }
        MessageSendLog saved = repo.save(MessageSendLog.builder()
            .academy(a)
            .kind(kind)
            .provider(sendResult.provider())
            .targetLabel(req.getTargetLabel())
            .title(req.getTitle())
            .bodyPreview(req.getBodyPreview())
            .recipientCount(req.getRecipientCount())
            .messageType(sendResult.messageType())
            .deductedPoints(sendResult.deductedPoints())
            .remainingPoints(sendResult.remainingPoints())
            .build());
        return toResponse(saved);
    }

    private SendResult sendIfRequested(Long academyId, MessageSendLogRequest req) {
        if (req.getMessageType() == null || req.getMessageType().isBlank()) {
            return new SendResult(MessageGatewayProvider.ALIGO, null, null, null);
        }
        List<String> recipientPhones = normalizePhones(req.getRecipientPhones());
        if (recipientPhones.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recipientPhones는 최소 1개 이상 필요합니다.");
        }
        String sendNo = normalizePhone(req.getSendNo());
        if (sendNo == null || sendNo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sendNo(발신번호)는 필수입니다.");
        }

        String messageType = req.getMessageType().trim().toUpperCase(Locale.ROOT);
        String body = req.getBody() != null && !req.getBody().isBlank() ? req.getBody() : req.getBodyPreview();

        // 실제 발송 전에 포인트 선차감(부족 시 예외로 발송 차단)
        AdminBillingService.SmsPointDeductionResult deduction =
            adminBillingService.deductSmsPoints(academyId, messageType, recipientPhones.get(0), recipientPhones.size());

        AligoSmsSendRequest request = new AligoSmsSendRequest();
        request.setSender(sendNo);
        request.setReceiver(String.join(",", recipientPhones));
        request.setMsg(body);
        request.setTitle(req.getTitle());

        switch (messageType) {
            case "KAKAO_ALIMTALK", "PAYMENT_SMS", "SMS" -> aligoSmsService.sendSms(request);
            case "LMS" -> aligoSmsService.sendLms(request);
            case "MMS" -> {
                if (req.getAttachFiles() == null || req.getAttachFiles().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "알리고 MMS 발송에는 attachFiles가 필요합니다.");
                }
                List<AligoSmsService.Base64Image> files = req.getAttachFiles().stream()
                    .filter(Objects::nonNull)
                    .map(f -> new AligoSmsService.Base64Image(f.getFileName(), f.getFileBodyBase64()))
                    .toList();
                aligoSmsService.sendMmsBase64(request, files);
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "messageType은 KAKAO_ALIMTALK, PAYMENT_SMS, SMS, LMS, MMS 중 하나여야 합니다.");
        }
        return new SendResult(
            MessageGatewayProvider.ALIGO,
            deduction.messageType(),
            deduction.deductedPoints(),
            deduction.remainingPoints()
        );
    }

    private List<String> normalizePhones(List<String> phones) {
        if (phones == null) {
            return List.of();
        }
        return phones.stream()
            .map(this::normalizePhone)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.replaceAll("[^0-9]", "");
        return normalized.isBlank() ? null : normalized;
    }

    private MessageSendLogResponse toResponse(MessageSendLog e) {
        return MessageSendLogResponse.builder()
            .id(e.getId())
            .kind(e.getKind().name())
            .provider(e.getProvider() != null ? e.getProvider().name() : null)
            .targetLabel(e.getTargetLabel())
            .title(e.getTitle())
            .bodyPreview(e.getBodyPreview())
            .recipientCount(e.getRecipientCount())
            .messageType(e.getMessageType())
            .deductedPoints(e.getDeductedPoints())
            .remainingPoints(e.getRemainingPoints())
            .createdAt(e.getCreatedAt())
            .build();
    }

    private record SendResult(
        MessageGatewayProvider provider,
        String messageType,
        Integer deductedPoints,
        Integer remainingPoints
    ) {}
}
