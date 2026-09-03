package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class CounselorResponse {
    private Long id;
    private String name;
    private String role;
}
