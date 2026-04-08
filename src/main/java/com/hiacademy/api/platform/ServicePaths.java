package com.hiacademy.api.platform;

/**
 * 멀티 서비스 공통 API에서 각 제품군의 서블릿 경로 접두사.
 * <p>
 * {@code server.servlet.context-path} 가 {@code /api} 일 때 전체 URL 예:<br>
 * {@code /api}{@link #ACADEMY}{@code /admin/...}
 */
public final class ServicePaths {

    private ServicePaths() {}

    /**
     * 공통 플랫폼 접두사 — 서비스 목록, Actuator 헬스 등 인프라·메타 API.
     * <p>
     * 헬스(Actuator): {@code /api} + {@link #PLATFORM} + {@code /health} → {@code /api/platform/health}
     */
    public static final String PLATFORM = "/platform";

    /** Actuator health 전체 서블릿 경로(컨텍스트 제외). {@link #PLATFORM} + {@code /health}. */
    public static final String PLATFORM_HEALTH = PLATFORM + "/health";

    /** 학원(academy) 서비스 — 어드민·학부모 API. */
    public static final String ACADEMY = "/academy";

    /** 디브릿지허브 홈페이지 전용 API 접두사. */
    public static final String HOMEPAGE = "/homepage";

    // 다른 제품군 추가 시: public static final String OTHER = "/other-service";
}
