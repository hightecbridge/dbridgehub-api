package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class NoticeAttachmentResponse {
    private Long id;
    private String fileName;
    private String contentType;
    private Long sizeBytes;
    private String data;
    private int sortOrder;
}
