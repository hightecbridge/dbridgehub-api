package com.hiacademy.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "expo.push")
public class ExpoPushProperties {
    /** Expo Push API (기본값: 공식 엔드포인트) */
    private String apiUrl = "https://exp.host/--/api/v2/push/send";
    /** false 이면 공지 발송 시 푸시를 보내지 않음 */
    private boolean enabled = true;
}
