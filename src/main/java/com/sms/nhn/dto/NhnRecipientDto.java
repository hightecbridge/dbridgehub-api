package com.sms.nhn.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Map;

/** NHN SMS/MMS 수신자 1건 ({@code recipientList} 항목). */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NhnRecipientDto {

    private String recipientNo;
    /** 기본 82(한국) */
    private String countryCode;
    private String internationalRecipientNo;
    private Map<String, Object> templateParameter;
    private String recipientGroupingKey;
}
