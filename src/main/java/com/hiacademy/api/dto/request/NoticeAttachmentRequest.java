package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NoticeAttachmentRequest {
    @NotBlank private String fileName;
    private String contentType;
    private Long sizeBytes;
    /** data URL (data:...;base64,...) 또는 base64 본문 */
    @NotBlank private String data;
}
