package com.sms.aligo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sms.aligo.config.AligoSmsProperties;
import com.sms.aligo.dto.AligoSmsSendRequest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class AligoSmsService {
    private final AligoSmsProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AligoSmsService(AligoSmsProperties properties, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public JsonNode sendSms(AligoSmsSendRequest request) {
        return send(request, "SMS", List.of());
    }

    public JsonNode sendLms(AligoSmsSendRequest request) {
        return send(request, "LMS", List.of());
    }

    public JsonNode sendMms(AligoSmsSendRequest request, List<MultipartFile> images) {
        return send(request, "MMS", toFileParts(images));
    }

    /** 모바일 웹 어드민처럼 base64 문자열을 받은 경우 사용합니다. */
    public JsonNode sendMmsBase64(AligoSmsSendRequest request, List<Base64Image> images) {
        return send(request, "MMS", toFilePartsFromBase64(images));
    }

    /** 알리고 알림톡(AT) 발송. */
    public JsonNode sendAlimtalk(AligoSmsSendRequest request, String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "알림톡 발송에는 templateCode가 필요합니다.");
        }
        validateKakaoConfig();
        String url = properties.getKakaoBaseUrl() + "/akv10/alimtalk/send/";
        String sender = normalizePhone(request.getSender());
        String receiver = normalizeReceiver(request.getReceiver());

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("apikey", properties.getApiKey());
        form.add("userid", properties.getUserId());
        form.add("senderkey", properties.getSenderKey());
        form.add("tpl_code", templateCode.trim());
        form.add("sender", sender);
        form.add("receiver_1", receiver);
        form.add("subject_1", request.getTitle() != null ? request.getTitle() : "");
        form.add("message_1", request.getMsg());
        if (properties.getTestmodeYn() != null && !properties.getTestmodeYn().isBlank()) {
            form.add("testMode", properties.getTestmodeYn());
        }

        JsonNode root = callApi(url, form);
        int code = root.path("code").asInt(-99999);
        if (code < 0) {
            throw new ResponseStatusException(BAD_GATEWAY, root.path("message").asText("알리고 알림톡 발송 실패"));
        }
        return root;
    }

    private JsonNode send(AligoSmsSendRequest request, String msgType, List<AligoFilePart> images) {
        validateConfig();
        String url = properties.getBaseUrl() + "/send/";

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("key", properties.getApiKey());
        form.add("user_id", properties.getUserId());
        form.add("sender", normalizePhone(request.getSender()));
        form.add("receiver", normalizeReceiver(request.getReceiver()));
        form.add("msg", request.getMsg());
        form.add("msg_type", msgType);
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            form.add("title", request.getTitle());
        }
        if (request.getDestination() != null && !request.getDestination().isBlank()) {
            form.add("destination", request.getDestination());
        }
        if (request.getRdate() != null && !request.getRdate().isBlank()) {
            form.add("rdate", request.getRdate());
        }
        if (request.getRtime() != null && !request.getRtime().isBlank()) {
            form.add("rtime", request.getRtime());
        }
        if (properties.getTestmodeYn() != null && !properties.getTestmodeYn().isBlank()) {
            form.add("testmode_yn", properties.getTestmodeYn());
        }

        addImages(form, images);

        JsonNode root = callApi(url, form);

        int resultCode = root.path("result_code").asInt(-99999);
        if (resultCode < 0) {
            throw new ResponseStatusException(BAD_GATEWAY, root.path("message").asText("알리고 발송 실패"));
        }
        return root;
    }

    private JsonNode callApi(String url, MultiValueMap<String, Object> form) {
        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(url, new HttpEntity<>(form, defaultMultipartHeaders()), String.class);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_GATEWAY, "알리고 API 호출 실패: " + e.getMessage());
        }

        String raw = response.getBody();
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "알리고 API 응답이 비어 있습니다.");
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_GATEWAY, "알리고 API 응답 파싱 실패: " + e.getMessage());
        }
    }

    private HttpHeaders defaultMultipartHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return headers;
    }

    private void addImages(MultiValueMap<String, Object> form, List<AligoFilePart> images) {
        List<AligoFilePart> validImages = new ArrayList<>();
        for (AligoFilePart image : images) {
            if (image == null || image.bytes().length == 0) {
                continue;
            }
            validImages.add(image);
        }
        if (validImages.size() > 3) {
            throw new ResponseStatusException(BAD_REQUEST, "MMS 이미지는 최대 3개까지 첨부할 수 있습니다.");
        }

        for (int i = 0; i < validImages.size(); i++) {
            AligoFilePart image = validImages.get(i);
            String key = "image" + (i + 1);
            try {
                form.add(key, toFilePart(key, image));
            } catch (Exception e) {
                throw new ResponseStatusException(BAD_REQUEST, "이미지 첨부 처리 실패: " + e.getMessage());
            }
        }
    }

    private HttpEntity<ByteArrayResource> toFilePart(String fieldName, AligoFilePart file) {
        String filename = file.filename() != null ? file.filename() : "attachment";
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentDisposition(ContentDisposition
            .builder("form-data")
            .name(fieldName)
            .filename(filename)
            .build());
        if (file.contentType() != null && !file.contentType().isBlank()) {
            partHeaders.setContentType(MediaType.parseMediaType(file.contentType()));
        }

        ByteArrayResource resource = new ByteArrayResource(file.bytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        return new HttpEntity<>(resource, partHeaders);
    }

    private void validateConfig() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "aligo.sms.api-key 가 설정되지 않았습니다.");
        }
        if (properties.getUserId() == null || properties.getUserId().isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "aligo.sms.user-id 가 설정되지 않았습니다.");
        }
    }

    private void validateKakaoConfig() {
        validateConfig();
        if (properties.getSenderKey() == null || properties.getSenderKey().isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "aligo.sms.sender-key 가 설정되지 않았습니다.");
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            throw new ResponseStatusException(BAD_REQUEST, "sender는 필수입니다.");
        }
        String normalized = phone.replaceAll("[^0-9]", "");
        if (normalized.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "sender가 올바르지 않습니다.");
        }
        return normalized;
    }

    private String normalizeReceiver(String receiver) {
        String[] parts = receiver.split(",");
        List<String> normalized = new ArrayList<>();
        for (String part : parts) {
            String n = part.replaceAll("[^0-9]", "");
            if (!n.isBlank()) {
                normalized.add(n);
            }
        }
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "receiver가 올바르지 않습니다.");
        }
        return String.join(",", normalized);
    }

    private List<AligoFilePart> toFileParts(List<MultipartFile> images) {
        if (images == null || images.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "MMS 발송에는 최소 1개의 이미지가 필요합니다.");
        }
        List<AligoFilePart> parts = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                continue;
            }
            try {
                parts.add(new AligoFilePart(image.getOriginalFilename(), image.getContentType(), image.getBytes()));
            } catch (Exception e) {
                throw new ResponseStatusException(BAD_REQUEST, "이미지 첨부 처리 실패: " + e.getMessage());
            }
        }
        if (parts.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "MMS 발송에는 최소 1개의 이미지가 필요합니다.");
        }
        return parts;
    }

    private List<AligoFilePart> toFilePartsFromBase64(List<Base64Image> images) {
        if (images == null || images.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "MMS 발송에는 최소 1개의 이미지가 필요합니다.");
        }
        List<AligoFilePart> parts = new ArrayList<>();
        for (Base64Image image : images) {
            if (image == null || image.fileBodyBase64() == null || image.fileBodyBase64().isBlank()) {
                continue;
            }
            try {
                byte[] bytes = Base64.getDecoder().decode(image.fileBodyBase64());
                parts.add(new AligoFilePart(image.fileName(), null, bytes));
            } catch (Exception e) {
                throw new ResponseStatusException(BAD_REQUEST, "첨부파일 base64 디코딩 실패: " + e.getMessage());
            }
        }
        if (parts.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "MMS 발송에는 최소 1개의 이미지가 필요합니다.");
        }
        return parts;
    }

    private record AligoFilePart(String filename, String contentType, byte[] bytes) {}

    public record Base64Image(String fileName, String fileBodyBase64) {}
}
