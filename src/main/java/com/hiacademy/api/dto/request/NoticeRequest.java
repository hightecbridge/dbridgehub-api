package com.hiacademy.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class NoticeRequest {
    @NotBlank private String title;
    @NotBlank private String body;
    @NotNull private List<String> targets;
    /** @deprecated files 사용 — 하위호환용 단일 이미지 */
    private String imageUrl;
    private String date;
    /** 첨부파일 (최대 10개) */
    @Valid
    @Size(max = 10, message = "첨부파일은 최대 10개까지 등록할 수 있습니다.")
    private List<NoticeAttachmentRequest> files;
}
