package com.hiacademy.api.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Docker 실행 시 실제로 어떤 JDBC URL 로 붙는지 확인용 (localhost 로 잡히면 설정이 잘못된 것).
 */
@Configuration
@Profile("docker")
public class DockerDataSourceLogger {

    private static final Logger log = LoggerFactory.getLogger(DockerDataSourceLogger.class);

    @Bean
    ApplicationRunner logDockerJdbcUrl(DataSource dataSource) {
        return args -> {
            if (dataSource instanceof HikariDataSource h) {
                log.info("[docker] 실제 DB 연결 URL = {}", h.getJdbcUrl());
            }
        };
    }
}
