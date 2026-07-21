package com.hiacademy.api.service;

import com.hiacademy.api.dto.response.MessageSenderInfoResponse;
import com.hiacademy.api.repository.MessageSenderProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessageSenderResolverService {
    private final MessageSenderProfileRepository repo;

    @Value("${app.message.default-sender-number:}")
    private String fallbackSenderNumber;

    public MessageSenderResolverService(MessageSenderProfileRepository repo) {
        this.repo = repo;
    }

    /**
     * 로그인한 어드민 기준 발신번호 결정.
     * 1) message_sender_profiles 에 등록된 계정 전용 번호
     * 2) 공통 발신번호(admin_user_id IS NULL)
     * 3) application.yml app.message.default-sender-number
     */
    @Transactional(readOnly = true)
    public String resolveForUser(Long adminUserId) {
        return resolveInfoForUser(adminUserId).senderNumber();
    }

    @Transactional(readOnly = true)
    public MessageSenderInfo resolveInfoForUser(Long adminUserId) {
        if (adminUserId != null) {
            var override = repo.findFirstByAdminUserIdAndActiveTrue(adminUserId);
            if (override.isPresent()) {
                return new MessageSenderInfo(
                    requireNormalized(override.get().getSenderNumber(), "계정 전용 발신번호"),
                    "USER"
                );
            }
        }

        var common = repo.findFirstByAdminUserIdIsNullAndActiveTrueOrderByIdAsc();
        if (common.isPresent()) {
            return new MessageSenderInfo(
                requireNormalized(common.get().getSenderNumber(), "공통 발신번호"),
                "COMMON"
            );
        }

        String fallback = normalizePhone(fallbackSenderNumber);
        if (fallback != null && !fallback.isBlank()) {
            return new MessageSenderInfo(fallback, "DEFAULT");
        }

        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "공통 발신번호가 설정되어 있지 않습니다. message_sender_profiles 테이블에 공통 번호(admin_user_id=NULL)를 등록해 주세요."
        );
    }

    public MessageSenderInfoResponse toResponse(MessageSenderInfo info) {
        return MessageSenderInfoResponse.builder()
            .senderNumber(info.senderNumber())
            .source(info.source())
            .sourceLabel(sourceLabel(info.source()))
            .build();
    }

    private static String sourceLabel(String source) {
        return switch (source) {
            case "USER" -> "계정 전용 발신번호";
            case "COMMON" -> "공통 발신번호";
            case "DEFAULT" -> "기본 발신번호";
            default -> "발신번호";
        };
    }

    public record MessageSenderInfo(String senderNumber, String source) {}

    private String requireNormalized(String raw, String label) {
        String normalized = normalizePhone(raw);
        if (normalized == null || normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " 형식이 올바르지 않습니다.");
        }
        return normalized;
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.replaceAll("[^0-9]", "");
        return normalized.isBlank() ? null : normalized;
    }
}
