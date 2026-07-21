package com.hiacademy.api.schema;

import com.hiacademy.api.entity.MessageSenderProfile;
import com.hiacademy.api.repository.MessageSenderProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공통 발신번호가 DB에 없으면 app.message.default-sender-number 로 1건 등록합니다.
 */
@Component
public class MessageSenderProfileInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MessageSenderProfileInitializer.class);

    private final MessageSenderProfileRepository repo;

    @Value("${app.message.default-sender-number:}")
    private String defaultSenderNumber;

    public MessageSenderProfileInitializer(MessageSenderProfileRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repo.findFirstByAdminUserIdIsNullAndActiveTrueOrderByIdAsc().isPresent()) {
            return;
        }
        String normalized = normalizePhone(defaultSenderNumber);
        if (normalized == null || normalized.isBlank()) {
            log.warn("[MessageSender] 공통 발신번호가 DB에 없고 default-sender-number 도 비어 있습니다.");
            return;
        }
        repo.save(MessageSenderProfile.builder()
            .adminUserId(null)
            .senderNumber(normalized)
            .active(true)
            .memo("공통 발신번호 (자동 등록)")
            .build());
        log.info("[MessageSender] 공통 발신번호 등록: {}", normalized);
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return null;
        }
        String normalized = phone.replaceAll("[^0-9]", "");
        return normalized.isBlank() ? null : normalized;
    }
}
