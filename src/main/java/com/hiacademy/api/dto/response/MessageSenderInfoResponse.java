package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageSenderInfoResponse {
  /** 숫자만 (예: 01050299455) */
  private String senderNumber;
  /** COMMON | USER | DEFAULT */
  private String source;
  /** 화면 표시용 라벨 */
  private String sourceLabel;
}
