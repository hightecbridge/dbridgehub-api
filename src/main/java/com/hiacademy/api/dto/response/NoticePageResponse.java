package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class NoticePageResponse {
    private List<NoticeResponse> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}

