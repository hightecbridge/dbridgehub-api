package com.hiacademy.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiacademy.api.config.ExpoPushProperties;
import com.hiacademy.api.entity.ParentPushToken;
import com.hiacademy.api.entity.Student;
import com.hiacademy.api.repository.ParentPushTokenRepository;
import com.hiacademy.api.repository.ParentRepository;
import com.hiacademy.api.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ExpoPushService {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushService.class);
    private static final int BATCH = 100;
    private static final int BODY_PREVIEW_MAX = 160;

    private final ExpoPushProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ParentRepository parentRepo;
    private final StudentRepository studentRepo;
    private final ParentPushTokenRepository pushTokenRepo;

    /**
     * 공지 등록 후 대상 학부모에게 Expo Push 전송.
     * targets에 "전체"가 있거나 비어 있으면 학원 전체 학부모, 아니면 해당 반(classroom.name)에 속한 학생의 학부모.
     */
    public void sendNoticeCreated(Long academyId, Long noticeId, String title, String body, List<String> targets) {
        if (!props.isEnabled()) {
            log.debug("[ExpoPush] disabled, skip noticeId={}", noticeId);
            return;
        }
        Set<Long> parentIds = resolveParentIds(academyId, targets);
        if (parentIds.isEmpty()) {
            log.info("[ExpoPush] no parents for academyId={} noticeId={}", academyId, noticeId);
            return;
        }
        List<ParentPushToken> tokens = pushTokenRepo.findAllByParent_IdIn(parentIds);
        if (tokens.isEmpty()) {
            log.info("[ExpoPush] no push tokens for {} parents (academyId={} noticeId={})", parentIds.size(), academyId, noticeId);
            return;
        }
        String preview = truncateBody(body);
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ParentPushToken pt : tokens) {
            // FCM(Android)은 data 값이 모두 문자열이어야 함 — 숫자 그대로 넣으면 티켓 error 가능
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("type", "notice");
            data.put("noticeId", String.valueOf(noticeId));
            data.put("academyId", String.valueOf(academyId));

            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("to", pt.getExpoPushToken());
            msg.put("title", title != null ? title : "공지사항");
            msg.put("body", preview);
            msg.put("sound", "default");
            msg.put("priority", "high");
            // channelId 생략 시 Expo가 기본 채널 생성(문서). "default" 지정은 앱에 채널이 없으면 표시 실패 가능
            msg.put("data", data);
            messages.add(msg);
        }
        sendBatches(messages);
    }

    private Set<Long> resolveParentIds(Long academyId, List<String> targets) {
        if (isBroadcast(targets)) {
            return new HashSet<>(parentRepo.findAllByAcademy_IdOrderByCreatedAtDesc(academyId).stream()
                .map(p -> p.getId()).toList());
        }
        Set<String> wanted = new HashSet<>();
        for (String t : targets) {
            if (t != null && !t.isBlank()) wanted.add(t.trim());
        }
        if (wanted.isEmpty()) {
            return new HashSet<>(parentRepo.findAllByAcademy_IdOrderByCreatedAtDesc(academyId).stream()
                .map(p -> p.getId()).toList());
        }
        Set<Long> ids = new HashSet<>();
        for (Student s : studentRepo.findAllByAcademyIdWithClassroom(academyId)) {
            if (s.getClassroom() == null) continue;
            String className = s.getClassroom().getName();
            if (className != null && wanted.contains(className)) {
                ids.add(s.getParent().getId());
            }
        }
        return ids;
    }

    private static boolean isBroadcast(List<String> targets) {
        if (targets == null || targets.isEmpty()) return true;
        for (String t : targets) {
            if (t != null && "전체".equals(t.trim())) return true;
        }
        return false;
    }

    private static String truncateBody(String body) {
        if (body == null) return "";
        String s = body.replace('\n', ' ').trim();
        if (s.length() <= BODY_PREVIEW_MAX) return s;
        return s.substring(0, BODY_PREVIEW_MAX - 1) + "…";
    }

    private void sendBatches(List<Map<String, Object>> messages) {
        for (int i = 0; i < messages.size(); i += BATCH) {
            int end = Math.min(i + BATCH, messages.size());
            List<Map<String, Object>> chunk = messages.subList(i, end);
            postChunk(chunk);
        }
    }

    private void postChunk(List<Map<String, Object>> chunk) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            // Expo POST https://exp.host/--/api/v2/push/send — body must be a JSON array of message objects, not { "messages": [...] }
            HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(chunk, headers);

            String raw = restTemplate.postForObject(props.getApiUrl(), entity, String.class);
            logExpoResponse(raw);
        } catch (Exception e) {
            log.warn("[ExpoPush] request failed: {}", e.getMessage());
        }
    }

    private void logExpoResponse(String raw) {
        if (raw == null || raw.isBlank()) return;
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) return;
            int ok = 0, err = 0;
            for (JsonNode item : data) {
                String status = item.path("status").asText("");
                if ("ok".equals(status)) {
                    ok++;
                } else {
                    err++;
                    log.warn("[ExpoPush] ticket error: status={} message={} details={}",
                        status,
                        item.path("message").asText(""),
                        item.has("details") ? item.get("details").toString() : "{}");
                }
            }
            log.info("[ExpoPush] batch result ok={} error={}", ok, err);
            if (err > 0) {
                log.warn("[ExpoPush] full response (for debugging): {}", raw);
            }
        } catch (Exception e) {
            log.debug("[ExpoPush] could not parse response: {}", e.getMessage());
        }
    }
}
