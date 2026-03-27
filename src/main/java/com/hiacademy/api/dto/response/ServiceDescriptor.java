package com.hiacademy.api.dto.response;

/** 플랫폼에 등록된 개별 서비스(제품) 설명. */
public record ServiceDescriptor(
        String id,
        String pathPrefix,
        String name,
        String description
) {}
