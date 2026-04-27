package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class MessageSendLogRequest {
    @NotBlank
    @Size(max = 20)
    private String kind; // CLASS | ALL | PAYMENT

    @NotBlank
    @Size(max = 200)
    private String targetLabel;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String bodyPreview;

    @NotNull
    @Min(0)
    private Integer recipientCount;

    /** 실제 발송 타입(SMS/LMS/MMS). 값이 없으면 기존처럼 로그만 저장합니다. */
    @Size(max = 10)
    private String messageType;

    /** 발신 번호 */
    @Size(max = 20)
    private String sendNo;

    /** 실제 발송 본문(로그 본문과 분리 가능). 없으면 bodyPreview 사용 */
    @Size(max = 4000)
    private String body;

    /** 실제 발송 대상 번호 목록 */
    private List<@Size(max = 30) String> recipientPhones;

    /** 기존에 업로드된 NHN 첨부 파일 ID 목록 */
    private List<Long> attachFileIdList;

    /** 신규 업로드할 파일(base64). MMS에서 사용 */
    @Valid
    private List<AttachmentFileRequest> attachFiles;

    @Data
    public static class AttachmentFileRequest {
        @NotBlank
        @Size(max = 100)
        private String fileName;

        @NotBlank
        @Size(max = 500000)
        private String fileBodyBase64;
    }
}
