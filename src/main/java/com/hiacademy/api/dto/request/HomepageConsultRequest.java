package com.hiacademy.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 디브릿지허브 홈페이지 "무료 상담 신청하기" 폼 요청 DTO.
 */
@Data
public class HomepageConsultRequest {

    /** 신청자 이름 */
    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    /** 연락처 (전화번호 등) */
    @NotBlank(message = "연락처는 필수입니다.")
    private String phone;

    /** 상담 요청 내용 (선택 가능) */
    private String message;

    /** 유입 경로나 페이지 식별자 (선택) */
    private String source;
}

