package com.hiacademy.api.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StudentWithdrawnAtSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StudentWithdrawnAtSchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public StudentWithdrawnAtSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE public.students ADD COLUMN IF NOT EXISTS withdrawn_at TIMESTAMP");
        } catch (Exception e) {
            log.warn("[StudentWithdrawnAt] skip: {}", e.getMessage());
        }
    }
}
