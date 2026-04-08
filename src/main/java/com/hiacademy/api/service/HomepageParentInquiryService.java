package com.hiacademy.api.service;

import com.hiacademy.api.dto.request.HomepageParentInquiryRequest;
import com.hiacademy.api.entity.HomepageParentInquiry;
import com.hiacademy.api.repository.HomepageParentInquiryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HomepageParentInquiryService {

    private static final Logger log = LoggerFactory.getLogger(HomepageParentInquiryService.class);
    private final HomepageParentInquiryRepository repo;

    public void create(HomepageParentInquiryRequest req) {
        HomepageParentInquiry saved = repo.save(HomepageParentInquiry.builder()
            .name(req.getName().trim())
            .phone(req.getPhone().trim())
            .childName(req.getChildName())
            .message(req.getMessage())
            .source(req.getSource())
            .build());
        log.info("[ParentInquiry] id={} name={}", saved.getId(), saved.getName());
    }
}
