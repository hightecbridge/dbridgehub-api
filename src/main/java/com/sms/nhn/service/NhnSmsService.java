package com.sms.nhn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.nhn.config.NhnSmsProperties;
import com.sms.nhn.dto.LmsSendRequest;
import com.sms.nhn.dto.MmsSendRequest;
import com.sms.nhn.dto.SmsSendRequest;
import com.sms.nhn.entity.SmsMessageType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class NhnSmsService {

    private static final MediaType NHN_JSON = MediaType.parseMediaType("application/json;charset=UTF-8");

    private final NhnSmsProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SmsSendHistoryService smsSendHistoryService;

    public NhnSmsService(
        NhnSmsProperties properties,
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        SmsSendHistoryService smsSendHistoryService
    ) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.smsSendHistoryService = smsSendHistoryService;
    }

    public JsonNode sendSms(SmsSendRequest request) {
        validateConfig();
        String url = properties.getBaseUrl() + "/sms/v3.0/appKeys/" + properties.getAppKey() + "/sender/sms";
        return postJson(SmsMessageType.SMS, url, request);
    }

    /** 장문 LMS — NHN API 경로는 MMS와 동일하며 본문에 첨부 ID가 없으면 장문 텍스트(LMS) 발송입니다. */
    public JsonNode sendLms(LmsSendRequest request) {
        validateConfig();
        String url = properties.getBaseUrl() + "/sms/v3.0/appKeys/" + properties.getAppKey() + "/sender/mms";
        return postJson(SmsMessageType.LMS, url, request);
    }

    public JsonNode sendMms(MmsSendRequest request) {
        validateConfig();
        String url = properties.getBaseUrl() + "/sms/v3.0/appKeys/" + properties.getAppKey() + "/sender/mms";
        return postJson(SmsMessageType.MMS, url, request);
    }

    /** NHN 첨부 파일 업로드: POST /attachfile/binaryUpload */
    public Long uploadAttachFile(String fileName, String createUser, String fileBodyBase64) {
        validateConfig();
        String url = properties.getBaseUrl() + "/sms/v3.0/appKeys/" + properties.getAppKey() + "/attachfile/binaryUpload";
        String requestBody = toJson(new UploadRequest(fileName, createUser, fileBodyBase64));
        JsonNode root = postRaw(url, requestBody);
        JsonNode header = root.path("header");
        if (!header.path("isSuccessful").asBoolean(false)) {
            String msg = header.path("resultMessage").asText("NHN 첨부 파일 업로드 실패");
            throw new ResponseStatusException(BAD_GATEWAY, msg);
        }
        JsonNode fileIdNode = root.path("body").path("data").path("fileId");
        if (!fileIdNode.isNumber()) {
            throw new ResponseStatusException(BAD_GATEWAY, "NHN 첨부 파일 업로드 응답에 fileId가 없습니다.");
        }
        return fileIdNode.asLong();
    }

    private JsonNode postJson(SmsMessageType messageType, String url, Object body) {
        String requestBody = toJson(body);

        JsonNode root;
        try {
            root = postRaw(url, requestBody);
        } catch (ResponseStatusException e) {
            smsSendHistoryService.saveFailure(messageType, url, requestBody, e.getReason());
            throw e;
        }
        String raw = root.toString();
        JsonNode header = root.path("header");
        smsSendHistoryService.saveSuccess(messageType, url, requestBody, raw, root);
        if (!header.path("isSuccessful").asBoolean(false)) {
            String msg = header.path("resultMessage").asText("NHN SMS 요청 실패");
            throw new ResponseStatusException(BAD_GATEWAY, msg);
        }
        return root;
    }

    private JsonNode postRaw(String url, String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(NHN_JSON);
        headers.set("X-Secret-Key", properties.getSecretKey());

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_GATEWAY, "NHN SMS API 호출 실패: " + e.getMessage());
        }

        String raw = response.getBody();
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "NHN SMS API 응답이 비어 있습니다.");
        }

        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_GATEWAY, "NHN SMS API 응답 파싱 실패: " + e.getMessage());
        }
    }

    private String toJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return "{\"error\":\"request-body-serialization-failed\"}";
        }
    }

    private void validateConfig() {
        if (properties.getAppKey() == null || properties.getAppKey().isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "nhn.sms.app-key 가 설정되지 않았습니다.");
        }
        if (properties.getSecretKey() == null || properties.getSecretKey().isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "nhn.sms.secret-key 가 설정되지 않았습니다.");
        }
    }

    private record UploadRequest(String fileName, String createUser, String fileBody) {}
}
