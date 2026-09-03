package com.hiacademy.api.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class MenuSettingsSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MenuSettingsSchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public MenuSettingsSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE public.academies ADD COLUMN IF NOT EXISTS menu_settings_json TEXT");
        } catch (Exception e) {
            log.warn("[MenuSettingsSchema] skip: {}", e.getMessage());
        }
    }
}
