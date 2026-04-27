package com.sms.nhn.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 장문 MMS 발송 요청 — {@link LmsSendRequest}와 동일하며, 콘솔에 업로드한 파일 ID 목록을 넣으면 첨부 MMS가 됩니다.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MmsSendRequest extends LmsSendRequest {

    /** 콘솔에서 등록한 첨부 파일 ID (jpg, 3개 이하 등 제한은 NHN 정책 따름) */
    private List<Long> attachFileIdList;
}
