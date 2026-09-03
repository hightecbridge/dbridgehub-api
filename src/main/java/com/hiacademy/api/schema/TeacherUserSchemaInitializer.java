package com.hiacademy.api.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TeacherUserSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TeacherUserSchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public TeacherUserSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE public.classrooms ADD COLUMN IF NOT EXISTS teacher_user_id BIGINT");
            jdbc.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'fk_classrooms_teacher_user'
                  ) THEN
                    ALTER TABLE public.classrooms
                      ADD CONSTRAINT fk_classrooms_teacher_user
                      FOREIGN KEY (teacher_user_id) REFERENCES public.users(id);
                  END IF;
                END $$;
                """);
        } catch (Exception e) {
            log.warn("[TeacherUserSchema] skip: {}", e.getMessage());
        }
    }
}
