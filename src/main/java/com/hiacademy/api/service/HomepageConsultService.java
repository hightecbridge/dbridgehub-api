package com.hiacademy.api.service;

import com.hiacademy.api.dto.request.HomepageConsultRequest;
import com.hiacademy.api.entity.HomepageConsult;
import com.hiacademy.api.repository.HomepageConsultRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 디브릿지허브 홈페이지 무료 상담 신청 저장용 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HomepageConsultService {

    private static final Logger log = LoggerFactory.getLogger(HomepageConsultService.class);

    private final HomepageConsultRepository repo;

    public void create(HomepageConsultRequest req) {
        HomepageConsult saved = repo.save(HomepageConsult.builder()
            .name(req.getName())
            .phone(req.getPhone())
            .message(req.getMessage())
            .source(req.getSource())
            .build());
        log.info("[HomepageConsult] created id={} name={} phone={}", saved.getId(), saved.getName(), saved.getPhone());
    }
}

