package com.sms.nhn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sms.nhn.entity.SmsMessageType;
import com.sms.nhn.entity.SmsSendHistory;
import com.sms.nhn.entity.SmsSendResult;
import com.sms.nhn.repository.SmsSendHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmsSendHistoryService {

    private final SmsSendHistoryRepository historyRepository;

    public SmsSendHistoryService(SmsSendHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional
    public void saveSuccess(
        SmsMessageType messageType,
        String requestUrl,
        String requestBody,
        String responseBody,
        JsonNode responseRoot
    ) {
        JsonNode header = responseRoot.path("header");
        JsonNode data = responseRoot.path("body").path("data");

        SmsSendHistory history = SmsSendHistory.builder()
            .messageType(messageType)
            .requestUrl(requestUrl)
            .requestBody(requestBody)
            .responseBody(responseBody)
            .requestId(textOrNull(data, "requestId"))
            .statusCode(textOrNull(data, "statusCode"))
            .resultCode(header.path("resultCode").isNumber() ? header.path("resultCode").asInt() : null)
            .resultMessage(textOrNull(header, "resultMessage"))
            .successful(header.path("isSuccessful").asBoolean(false))
            .recipientCount(data.path("sendResultList").isArray() ? data.path("sendResultList").size() : 0)
            .build();

        JsonNode sendResultList = data.path("sendResultList");
        if (sendResultList.isArray()) {
            for (JsonNode item : sendResultList) {
                SmsSendResult result = SmsSendResult.builder()
                    .recipientNo(textOrNull(item, "recipientNo"))
                    .recipientSeq(item.path("recipientSeq").isNumber() ? item.path("recipientSeq").asInt() : null)
                    .resultCode(item.path("resultCode").isNumber() ? item.path("resultCode").asInt() : null)
                    .resultMessage(textOrNull(item, "resultMessage"))
                    .build();
                history.addResult(result);
            }
        }

        historyRepository.save(history);
    }

    @Transactional
    public void saveFailure(
        SmsMessageType messageType,
        String requestUrl,
        String requestBody,
        String resultMessage
    ) {
        SmsSendHistory history = SmsSendHistory.builder()
            .messageType(messageType)
            .requestUrl(requestUrl)
            .requestBody(requestBody)
            .responseBody(null)
            .requestId(null)
            .statusCode(null)
            .resultCode(null)
            .resultMessage(resultMessage)
            .successful(false)
            .recipientCount(0)
            .build();

        historyRepository.save(history);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        String text = v.asText();
        return text == null || text.isBlank() ? null : text;
    }
}
