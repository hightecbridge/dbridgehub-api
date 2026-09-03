package com.hiacademy.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiacademy.api.config.ExpoPushProperties;
import com.hiacademy.api.entity.ParentPushToken;
import com.hiacademy.api.entity.Student;
import com.hiacademy.api.repository.ParentPushTokenRepository;
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
    private final StudentRepository studentRepo;
    private final ParentPushTokenRepository pushTokenRepo;

    /**
     * 공지 등록 후 대상 학부모에게 Expo Push 전송.
     * targets에 "전체"가 있거나 비어 있으면 학원 전체 학부모, 아니면 해당 반(classroom.name)에 속한 학생의 학부모.
     */
    public void sendNoticeCreated(Long academyId, Long noticeId, String title, String body, List<String> targets) {
        sendTargetedPush(academyId, title != null ? title : "공지사항", body, targets, "notice", "noticeId", noticeId);
    }

    public void sendClassNoticeCreated(Long academyId, Long classNoticeId, String title, String body, List<String> targets) {
        sendTargetedPush(academyId, title != null ? title : "클래스 알림장", body, targets, "class_notice", "classNoticeId", classNoticeId);
    }

    private void sendTargetedPush(
        Long academyId, String title, String body, List<String> targets,
        String type, String idKey, Long id
    ) {
        if (!props.isEnabled()) {
            log.debug("[ExpoPush] disabled, skip type={} id={}", type, id);
            return;
        }
        Set<Long> studentIds = resolveStudentIds(academyId, targets);
        if (studentIds.isEmpty()) {
            log.info("[ExpoPush] no students for academyId={} type={} id={}", academyId, type, id);
            return;
        }
        List<ParentPushToken> tokens = pushTokenRepo.findAllByStudent_IdIn(studentIds);
        if (tokens.isEmpty()) {
            log.info("[ExpoPush] no push tokens for {} students (academyId={} type={} id={})", studentIds.size(), academyId, type, id);
            return;
        }
        String preview = truncateBody(body);
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ParentPushToken pt : tokens) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("type", type);
            data.put(idKey, String.valueOf(id));
            data.put("academyId", String.valueOf(academyId));

            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("to", pt.getExpoPushToken());
            msg.put("title", title);
            msg.put("body", preview);
            msg.put("sound", "default");
            msg.put("priority", "high");
            msg.put("data", data);
            messages.add(msg);
        }
        sendBatches(messages);
    }

    private Set<Long> resolveStudentIds(Long academyId, List<String> targets) {
        List<Student> all = studentRepo.findAllByAcademyIdWithClassroom(academyId);
        if (isBroadcast(targets)) {
            return all.stream().map(Student::getId).collect(java.util.stream.Collectors.toSet());
        }
        Set<String> wanted = new HashSet<>();
        for (String t : targets) {
            if (t != null && !t.isBlank()) wanted.add(t.trim());
        }
        if (wanted.isEmpty()) {
            return all.stream().map(Student::getId).collect(java.util.stream.Collectors.toSet());
        }
        Set<Long> ids = new HashSet<>();
        for (Student s : all) {
            if (s.getClassroom() == null) continue;
            String className = s.getClassroom().getName();
            if (className != null && wanted.contains(className)) {
                ids.add(s.getId());
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
