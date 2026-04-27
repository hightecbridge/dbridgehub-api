package com.sms.aligo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AligoSmsSendRequest {
    @NotBlank
    @Size(max = 16)
    private String sender;

    /** 콤마(,)로 구분된 수신번호 목록 */
    @NotBlank
    private String receiver;

    @NotBlank
    @Size(max = 2000)
    private String msg;

    @Size(max = 44)
    private String title;

    /** 20260423 형식 */
    @Size(min = 8, max = 8)
    private String rdate;

    /** 1715 형식 */
    @Size(min = 4, max = 4)
    private String rtime;

    private String destination;
}
