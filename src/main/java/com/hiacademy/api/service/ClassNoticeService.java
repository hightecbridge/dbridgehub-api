package com.hiacademy.api.service;

import com.hiacademy.api.dto.request.NoticeAttachmentRequest;
import com.hiacademy.api.dto.request.NoticeRequest;
import com.hiacademy.api.dto.response.NoticePageResponse;
import com.hiacademy.api.dto.response.NoticeResponse;
import com.hiacademy.api.entity.*;
import com.hiacademy.api.repository.AcademyRepository;
import com.hiacademy.api.repository.ClassNoticeRepository;
import com.hiacademy.api.repository.ClassRoomRepository;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service @Transactional
public class ClassNoticeService {
    private static final Logger log = LoggerFactory.getLogger(ClassNoticeService.class);
    private final ClassNoticeRepository classNoticeRepo;
    private final AcademyRepository academyRepo;
    private final ClassRoomRepository classRepo;
    private final AdminAccessService access;
    private final ExpoPushService expoPushSvc;

    public ClassNoticeService(
        ClassNoticeRepository classNoticeRepo,
        AcademyRepository academyRepo,
        ClassRoomRepository classRepo,
        AdminAccessService access,
        ExpoPushService expoPushSvc
    ) {
        this.classNoticeRepo = classNoticeRepo;
        this.academyRepo = academyRepo;
        this.classRepo = classRepo;
        this.access = access;
        this.expoPushSvc = expoPushSvc;
    }

    @Transactional(readOnly = true)
    public NoticePageResponse list(Authentication auth, int page, int size, String target, String q) {
        AdminAccessService.Scope scope = access.resolve(auth);
        String targetNorm = (target == null || target.isBlank()) ? null : target;
        String qNorm = (q == null || q.isBlank()) ? null : q;

        if (!scope.director() && targetNorm != null && !"전체".equals(targetNorm)) {
            Set<String> allowed = allowedClassNames(scope);
            if (!allowed.contains(targetNorm)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 반만 조회할 수 있습니다.");
            }
        }

        Pageable pageable = PageRequest.of(
            Math.max(0, page),
            Math.max(1, size),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Set<String> allowed = allowedClassNames(scope);
        boolean restrictTargets = !scope.director();
        List<String> allowedList = allowed.isEmpty() ? List.of("__none__") : List.copyOf(allowed);

        Page<ClassNotice> pageItems = classNoticeRepo.search(
            scope.academyId(), targetNorm, qNorm, restrictTargets, allowedList, pageable);
        pageItems.getContent().forEach(n -> {
            Hibernate.initialize(n.getTargets());
            Hibernate.initialize(n.getAttachments());
        });

        List<NoticeResponse> content = pageItems.getContent().stream().map(Mapper::toClassNotice).toList();

        return NoticePageResponse.builder()
            .content(content)
            .totalElements(pageItems.getTotalElements())
            .totalPages(pageItems.getTotalPages())
            .page(pageItems.getNumber())
            .size(pageItems.getSize())
            .build();
    }

    public NoticeResponse create(Authentication auth, NoticeRequest req) {
        AdminAccessService.Scope scope = access.resolve(auth);
        validateTargets(scope, req.getTargets());

        Academy a = academyRepo.getReferenceById(scope.academyId());
        String dateStr = req.getDate();
        if (dateStr == null || dateStr.isBlank()) {
            dateStr = LocalDate.now().toString();
        }

        List<NoticeAttachmentRequest> fileReqs = req.getFiles() == null ? List.of() : req.getFiles();
        if (fileReqs.size() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "첨부파일은 최대 10개까지 등록할 수 있습니다.");
        }
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

        ClassNotice notice = ClassNotice.builder()
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
            notice.getAttachments().add(ClassNoticeAttachment.builder()
                .classNotice(notice)
                .fileName(name.length() > 255 ? name.substring(0, 255) : name)
                .contentType(fr.getContentType() != null ? fr.getContentType() : guessContentType(fr.getData(), "application/octet-stream"))
                .sizeBytes(fr.getSizeBytes())
                .data(fr.getData())
                .sortOrder(order++)
                .build());
        }

        ClassNotice saved = classNoticeRepo.save(notice);
        Hibernate.initialize(saved.getAttachments());
        try {
            expoPushSvc.sendClassNoticeCreated(
                scope.academyId(), saved.getId(), saved.getTitle(), saved.getBody(), saved.getTargets());
        } catch (Exception e) {
            log.warn("[ClassNoticeService] Expo push failed after create id={}: {}", saved.getId(), e.getMessage());
        }
        return Mapper.toClassNotice(saved);
    }

    public void delete(Authentication auth, Long id) {
        AdminAccessService.Scope scope = access.resolve(auth);
        ClassNotice notice = classNoticeRepo.findByIdAndAcademy_Id(id, scope.academyId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Hibernate.initialize(notice.getTargets());
        if (!scope.director()) {
            if (!visibleToTeacher(notice.getTargets(), allowedClassNames(scope))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 반 알림장만 삭제할 수 있습니다.");
            }
        }
        classNoticeRepo.delete(notice);
    }

    private void validateTargets(AdminAccessService.Scope scope, List<String> targets) {
        if (targets == null || targets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "발송 대상을 선택하세요.");
        }
        Set<String> allowed = allowedClassNames(scope);
        for (String t : targets) {
            if (t == null || t.isBlank()) continue;
            if ("전체".equals(t.trim())) {
                if (!scope.director()) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 반만 선택할 수 있습니다.");
                }
                continue;
            }
            if (!allowed.contains(t.trim())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 반만 선택할 수 있습니다.");
            }
        }
    }

    private Set<String> allowedClassNames(AdminAccessService.Scope scope) {
        List<ClassRoom> rooms = scope.director()
            ? classRepo.findAllByAcademy_IdOrderByCreatedAtAsc(scope.academyId())
            : classRepo.findAllByTeacherUser_IdAndAcademy_Id(scope.userId(), scope.academyId());
        return rooms.stream().map(ClassRoom::getName).collect(Collectors.toCollection(HashSet::new));
    }

    private static boolean visibleToTeacher(List<String> targets, Set<String> allowed) {
        if (targets == null || targets.isEmpty()) return false;
        for (String t : targets) {
            if (t == null) continue;
            if ("전체".equals(t.trim())) return true;
            if (allowed.contains(t.trim())) return true;
        }
        return false;
    }

    private static String guessContentType(String data, String fallback) {
        if (data == null) return fallback;
        if (data.startsWith("data:")) {
            int semi = data.indexOf(';');
            if (semi > 5) return data.substring(5, semi);
        }
        return fallback;
    }
}
