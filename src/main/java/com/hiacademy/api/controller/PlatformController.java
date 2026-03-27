package com.hiacademy.api.controller;

import com.hiacademy.api.dto.response.ApiResponse;
import com.hiacademy.api.dto.response.ServiceDescriptor;
import com.hiacademy.api.platform.ServicePaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 공통 플랫폼 API — 서비스 디스커버리 등.
 * 헬스 체크는 Spring Actuator가 동일 {@code /platform} 접두 하에 {@code /health}로 제공한다
 * ({@code management.endpoints.web.base-path}).
 */
@RestController
@RequestMapping(ServicePaths.PLATFORM)
public class PlatformController {

    @GetMapping("/services")
    public ApiResponse<List<ServiceDescriptor>> listServices() {
        return ApiResponse.ok(List.of(
                new ServiceDescriptor(
                        "academy",
                        ServicePaths.ACADEMY,
                        "Academy",
                        "학원 관리 REST API (어드민·학부모 앱)"
                )
        ));
    }
}
