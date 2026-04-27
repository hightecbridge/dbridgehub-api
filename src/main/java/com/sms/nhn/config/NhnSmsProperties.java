package com.sms.nhn.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * NHN Cloud Notification SMS v3.0 연동 설정.
 * <p>
 * 문서: <a href="https://docs.nhncloud.com/ko/Notification/SMS/ko/api-guide/">API 가이드</a>
 */
@ConfigurationProperties(prefix = "nhn.sms")
public class NhnSmsProperties {

    /** 콘솔에서 발급한 AppKey */
    private String appKey = "";
    /** 요청 헤더 {@code X-Secret-Key} */
    private String secretKey = "";
    /** 기본: {@code https://sms.api.nhncloudservice.com} */
    private String baseUrl = "https://sms.api.nhncloudservice.com";

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
