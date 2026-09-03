package com.hiacademy.api.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ClassNoticeSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ClassNoticeSchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public ClassNoticeSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS public.class_notices (
                  id BIGSERIAL PRIMARY KEY,
                  title VARCHAR(255) NOT NULL,
                  body TEXT NOT NULL,
                  image_url TEXT,
                  image_data TEXT,
                  date VARCHAR(32) NOT NULL,
                  academy_id BIGINT NOT NULL,
                  created_at TIMESTAMP
                )
                """);
            jdbc.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'fk_class_notices_academy'
                  ) THEN
                    ALTER TABLE public.class_notices
                      ADD CONSTRAINT fk_class_notices_academy
                      FOREIGN KEY (academy_id) REFERENCES public.academies(id) ON DELETE CASCADE;
                  END IF;
                END $$;
                """);
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS public.class_notice_targets (
                  class_notice_id BIGINT NOT NULL,
                  target VARCHAR(255)
                )
                """);
            jdbc.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'fk_class_notice_targets_notice'
                  ) THEN
                    ALTER TABLE public.class_notice_targets
                      ADD CONSTRAINT fk_class_notice_targets_notice
                      FOREIGN KEY (class_notice_id) REFERENCES public.class_notices(id) ON DELETE CASCADE;
                  END IF;
                END $$;
                """);
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS public.class_notice_attachments (
                  id BIGSERIAL PRIMARY KEY,
                  class_notice_id BIGINT NOT NULL,
                  file_name VARCHAR(255) NOT NULL,
                  content_type VARCHAR(120),
                  size_bytes BIGINT,
                  data TEXT NOT NULL,
                  sort_order INT NOT NULL DEFAULT 0
                )
                """);
            jdbc.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'fk_class_notice_attachments_notice'
                  ) THEN
                    ALTER TABLE public.class_notice_attachments
                      ADD CONSTRAINT fk_class_notice_attachments_notice
                      FOREIGN KEY (class_notice_id) REFERENCES public.class_notices(id) ON DELETE CASCADE;
                  END IF;
                END $$;
                """);
            jdbc.execute("""
                CREATE INDEX IF NOT EXISTS idx_class_notices_academy_id
                ON public.class_notices(academy_id)
                """);
            jdbc.execute("""
                CREATE INDEX IF NOT EXISTS idx_class_notice_attachments_notice_id
                ON public.class_notice_attachments(class_notice_id)
                """);
        } catch (Exception e) {
            log.warn("[ClassNoticeSchema] skip: {}", e.getMessage());
        }
    }
}
