package com.hiacademy.api.service;

import com.hiacademy.api.entity.Parent;
import com.hiacademy.api.entity.ParentPushToken;
import com.hiacademy.api.repository.ParentPushTokenRepository;
import com.hiacademy.api.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ParentPushTokenService {
    private static final Logger log = LoggerFactory.getLogger(ParentPushTokenService.class);

    private final ParentRepository parentRepo;
    private final ParentPushTokenRepository tokenRepo;

    @Transactional
    public void register(Long parentId, String expoPushToken) {
        if (expoPushToken == null || expoPushToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expoPushToken이 필요합니다.");
        }
        String token = expoPushToken.trim();
        Parent parent = parentRepo.findById(parentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        tokenRepo.findByExpoPushToken(token).ifPresentOrElse(
            existing -> {
                boolean changed = false;
                if (existing.getToken() == null || existing.getToken().isBlank()) {
                    existing.setToken(token);
                    changed = true;
                }
                if (!existing.getParent().getId().equals(parentId)) {
                    existing.setParent(parent);
                    existing.setAcademy(parent.getAcademy());
                    changed = true;
                }
                if (changed) {
                    tokenRepo.save(existing);
                    log.info("[ParentPushToken] updated token row for parentId={}", parentId);
                } else {
                    log.info("[ParentPushToken] token already exists for parentId={}", parentId);
                }
            },
            () -> {
                tokenRepo.save(ParentPushToken.builder()
                    .parent(parent)
                    .academy(parent.getAcademy())
                    .expoPushToken(token)
                    .token(token)
                    .build());
                log.info("[ParentPushToken] registered token for parentId={}", parentId);
            }
        );
    }

    @Transactional
    public void unregisterAll(Long parentId) {
        tokenRepo.deleteAllByParent_Id(parentId);
    }
}
