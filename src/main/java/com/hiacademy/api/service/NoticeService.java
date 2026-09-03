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
import com.hiacademy.api.dto.request.NoticeAttachmentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Service @Transactional
public class NoticeService {
    private static final Logger log = LoggerFactory.getLogger(NoticeService.class);
    private final NoticeRepository  noticeRepo;
    private final AcademyRepository academyRepo;
    private final ExpoPushService   expoPushSvc;
    public NoticeService(NoticeRepository n, AcademyRepository a, ExpoPushService expoPushSvc) {
        noticeRepo=n; academyRepo=a; this.expoPushSvc=expoPushSvc;
    }
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
        pageItems.getContent().forEach(n -> {
            Hibernate.initialize(n.getTargets());
            Hibernate.initialize(n.getAttachments());
        });

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

        List<NoticeAttachmentRequest> fileReqs = req.getFiles() == null ? List.of() : req.getFiles();
        if (fileReqs.size() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "첨부파일은 최대 10개까지 등록할 수 있습니다.");
        }

        // 하위호환: files 없이 imageUrl만 온 경우 1개 파일로 변환
        if (fileReqs.isEmpty() && req.getImageUrl() != null && !req.getImageUrl().isBlank()) {
            NoticeAttachmentRequest legacy = new NoticeAttachmentRequest();
            legacy.setFileName("image.jpg");
            legacy.setContentType(guessContentType(req.getImageUrl(), "image/jpeg"));
            legacy.setData(req.getImageUrl());
            fileReqs = List.of(legacy);
        }

        String rawImage = null;
        String imageUrl = null;
        String imageData = null;
        // 기존 단일 이미지 컬럼에도 첫 이미지 보관 (구버전 앱 호환)
        for (NoticeAttachmentRequest fr : fileReqs) {
            if (fr.getData() == null) continue;
            String ct = fr.getContentType() != null ? fr.getContentType() : guessContentType(fr.getData(), null);
            if (ct != null && ct.startsWith("image/")) {
                rawImage = fr.getData();
                break;
            }
        }
        if (rawImage == null && req.getImageUrl() != null && !req.getImageUrl().isBlank()) {
            rawImage = req.getImageUrl();
        }
        if (rawImage != null) {
            if (rawImage.startsWith("data:") || rawImage.length() > 255) {
                imageData = rawImage;
            } else {
                imageUrl = rawImage;
            }
        }

        NoticeItem notice = NoticeItem.builder()
            .title(req.getTitle()).body(req.getBody()).targets(req.getTargets())
            .imageUrl(imageUrl).imageData(imageData).date(dateStr).academy(a).build();

        int order = 0;
        for (NoticeAttachmentRequest fr : fileReqs) {
            if (fr.getData() == null || fr.getData().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "첨부파일 데이터가 비어 있습니다.");
            }
            if (fr.getData().length() > 8_000_000) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "첨부파일이 너무 큽니다. 파일당 약 5MB 이하로 올려 주세요.");
            }
            String name = fr.getFileName() == null || fr.getFileName().isBlank() ? "file" : fr.getFileName().trim();
            notice.getAttachments().add(NoticeAttachment.builder()
                .notice(notice)
                .fileName(name.length() > 255 ? name.substring(0, 255) : name)
                .contentType(fr.getContentType() != null ? fr.getContentType() : guessContentType(fr.getData(), "application/octet-stream"))
                .sizeBytes(fr.getSizeBytes())
                .data(fr.getData())
                .sortOrder(order++)
                .build());
        }

        NoticeItem saved = noticeRepo.save(notice);
        Hibernate.initialize(saved.getAttachments());
        try {
            expoPushSvc.sendNoticeCreated(academyId, saved.getId(), saved.getTitle(), saved.getBody(), saved.getTargets());
        } catch (Exception e) {
            log.warn("[NoticeService] Expo push failed after create noticeId={}: {}", saved.getId(), e.getMessage());
        }
        return Mapper.toNotice(saved);
    }

    private static String guessContentType(String data, String fallback) {
        if (data == null) return fallback;
        if (data.startsWith("data:")) {
            int semi = data.indexOf(';');
            if (semi > 5) return data.substring(5, semi);
        }
        return fallback;
    }
    public void delete(Long academyId, Long id) {
        noticeRepo.delete(noticeRepo.findByIdAndAcademy_Id(id,academyId)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }
}
