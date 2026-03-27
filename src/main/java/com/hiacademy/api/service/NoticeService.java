package com.hiacademy.api.service;
import com.hiacademy.api.dto.request.NoticeRequest;
import com.hiacademy.api.dto.response.*;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.Hibernate;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service @Transactional
public class NoticeService {
    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);
    private final NoticeRepository  noticeRepo;
    private final AcademyRepository academyRepo;
    public NoticeService(NoticeRepository n, AcademyRepository a) { noticeRepo=n; academyRepo=a; }
    @Transactional(readOnly=true)
    public NoticePageResponse list(Long academyId, int page, int size, String target, String q) {
        String targetNorm = (target == null || target.isBlank()) ? null : target;
        String qNorm = (q == null || q.isBlank()) ? null : q;

        Pageable pageable = PageRequest.of(
            Math.max(0, page),
            Math.max(1, size),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<NoticeItem> pageItems = noticeRepo.search(academyId, targetNorm, qNorm, pageable);

        // targets는 환경에 따라 lazy proxy로 동작할 수 있어, JSON 직렬화 전에 강제로 초기화
        pageItems.getContent().forEach(n -> Hibernate.initialize(n.getTargets()));

        List<NoticeResponse> content = pageItems.getContent().stream().map(Mapper::toNotice).toList();
        log.info("[NoticeService] list academyId={} page={} size={} totalElements={} contentCount={}",
            academyId, pageItems.getNumber(), pageItems.getSize(), pageItems.getTotalElements(), content.size());

        // 디버깅 편의: 특정 academyId 페이지가 0이면(실데이터/academyId mismatch) 전체도 함께 찍어둠
        if (pageItems.getTotalElements() == 0 && academyId != null && page == 0) {
            Page<NoticeItem> allPage = noticeRepo.findAllByOrderByCreatedAtDesc(Pageable.unpaged());
            long allCount = allPage.getTotalElements();
            log.info("[NoticeService] list debug: academyId={} had 0 totalElements; ALL totalElements={}", academyId, allCount);
        }

        return NoticePageResponse.builder()
            .content(content)
            .totalElements(pageItems.getTotalElements())
            .totalPages(pageItems.getTotalPages())
            .page(pageItems.getNumber())
            .size(pageItems.getSize())
            .build();
    }
    public NoticeResponse create(Long academyId, NoticeRequest req) {
        Academy a = academyRepo.getReferenceById(academyId);
        String dateStr = req.getDate();
        if (dateStr == null || dateStr.isBlank()) {
            dateStr = LocalDate.now().toString();
        }
        String rawImage = req.getImageUrl();
        String imageUrl = rawImage;
        String imageData = null;
        // data URL/base64 is often very large; store in TEXT/LOB column.
        if (rawImage != null && (rawImage.startsWith("data:") || rawImage.length() > 255)) {
            imageUrl = null;
            imageData = rawImage;
        }
        return Mapper.toNotice(noticeRepo.save(NoticeItem.builder()
            .title(req.getTitle()).body(req.getBody()).targets(req.getTargets())
            .imageUrl(imageUrl).imageData(imageData).date(dateStr).academy(a).build()));
    }
    public void delete(Long academyId, Long id) {
        noticeRepo.delete(noticeRepo.findByIdAndAcademy_Id(id,academyId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }
}
