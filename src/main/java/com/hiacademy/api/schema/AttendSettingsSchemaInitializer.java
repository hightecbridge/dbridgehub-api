package com.hiacademy.api.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AttendSettingsSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AttendSettingsSchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public AttendSettingsSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE public.academies ADD COLUMN IF NOT EXISTS exclude_holidays_from_attend BOOLEAN DEFAULT FALSE");
        } catch (Exception e) {
            log.warn("[AttendSettingsSchema] skip: {}", e.getMessage());
        }
    }
}
