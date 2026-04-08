package com.hiacademy.api.service;

import com.hiacademy.api.dto.request.HomepageSignupInquiryRequest;
import com.hiacademy.api.entity.HomepageSignupInquiry;
import com.hiacademy.api.repository.HomepageSignupInquiryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HomepageSignupInquiryService {

    private static final Logger log = LoggerFactory.getLogger(HomepageSignupInquiryService.class);
    private final HomepageSignupInquiryRepository repo;

    public void create(HomepageSignupInquiryRequest req) {
        HomepageSignupInquiry saved = repo.save(HomepageSignupInquiry.builder()
            .academyName(req.getAcademyName().trim())
            .name(req.getName().trim())
            .phone(req.getPhone().trim())
            .studentCount(req.getStudentCount())
            .message(req.getMessage())
            .source(req.getSource())
            .build());
        log.info("[SignupInquiry] id={} academy={} name={}", saved.getId(), saved.getAcademyName(), saved.getName());
    }
}
