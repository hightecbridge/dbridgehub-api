package com.hiacademy.api.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NoticeAttachmentSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(NoticeAttachmentSchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public NoticeAttachmentSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS public.notice_attachments (
                  id BIGSERIAL PRIMARY KEY,
                  notice_id BIGINT NOT NULL,
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
                    SELECT 1 FROM pg_constraint WHERE conname = 'fk_notice_attachments_notice'
                  ) THEN
                    ALTER TABLE public.notice_attachments
                      ADD CONSTRAINT fk_notice_attachments_notice
                      FOREIGN KEY (notice_id) REFERENCES public.notices(id) ON DELETE CASCADE;
                  END IF;
                END $$;
                """);
            jdbc.execute("""
                CREATE INDEX IF NOT EXISTS idx_notice_attachments_notice_id
                ON public.notice_attachments(notice_id)
                """);
        } catch (Exception e) {
            log.warn("[NoticeAttachmentSchema] skip: {}", e.getMessage());
        }
    }
}
