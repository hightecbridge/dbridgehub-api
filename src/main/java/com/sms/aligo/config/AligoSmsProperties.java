package com.sms.aligo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aligo.sms")
public class AligoSmsProperties {
    private String apiKey = "";
    private String userId = "";
    private String baseUrl = "https://apis.aligo.in";
    private String kakaoBaseUrl = "https://kakaoapi.aligo.in";
    private String senderKey = "";
    private String testmodeYn = "N";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getKakaoBaseUrl() {
        return kakaoBaseUrl;
    }

    public void setKakaoBaseUrl(String kakaoBaseUrl) {
        this.kakaoBaseUrl = kakaoBaseUrl;
    }

    public String getSenderKey() {
        return senderKey;
    }

    public void setSenderKey(String senderKey) {
        this.senderKey = senderKey;
    }

    public String getTestmodeYn() {
        return testmodeYn;
    }

    public void setTestmodeYn(String testmodeYn) {
        this.testmodeYn = testmodeYn;
    }
}
