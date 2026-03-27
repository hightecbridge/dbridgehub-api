package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 공통 API 게이트웰이 구동되는 Spring Boot 진입점 (여러 서비스 모듈을 동일 프로세스에서 제공). */
@SpringBootApplication
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
