package com.hiacademy.api.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConsultationSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ConsultationSchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public ConsultationSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("ALTER TABLE public.consultations ADD COLUMN IF NOT EXISTS academy_id BIGINT");
            jdbc.execute("ALTER TABLE public.consultations ADD COLUMN IF NOT EXISTS teacher_user_id BIGINT");
            jdbc.execute("ALTER TABLE public.consultations ADD COLUMN IF NOT EXISTS kind VARCHAR(20)");
            jdbc.execute("ALTER TABLE public.consultations ADD COLUMN IF NOT EXISTS prospect_name VARCHAR(100)");
            jdbc.execute("ALTER TABLE public.consultations ADD COLUMN IF NOT EXISTS prospect_phone VARCHAR(40)");
            jdbc.execute("ALTER TABLE public.consultations ADD COLUMN IF NOT EXISTS prospect_grade VARCHAR(40)");
            jdbc.execute("ALTER TABLE public.consultations ADD COLUMN IF NOT EXISTS prospect_parent_name VARCHAR(100)");

            jdbc.execute("UPDATE public.consultations SET kind = '재원생' WHERE kind IS NULL");
            jdbc.execute("""
                UPDATE public.consultations c
                SET academy_id = COALESCE(
                    (SELECT s.academy_id FROM public.students s WHERE s.id = c.student_id),
                    (SELECT p.academy_id FROM public.students s
                       JOIN public.parents p ON s.parent_id = p.id
                      WHERE s.id = c.student_id)
                )
                WHERE c.academy_id IS NULL AND c.student_id IS NOT NULL
                """);

            jdbc.execute("ALTER TABLE public.consultations ALTER COLUMN student_id DROP NOT NULL");

            jdbc.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'fk_consultations_academy'
                  ) THEN
                    ALTER TABLE public.consultations
                      ADD CONSTRAINT fk_consultations_academy
                      FOREIGN KEY (academy_id) REFERENCES public.academies(id);
                  END IF;
                  IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'fk_consultations_teacher_user'
                  ) THEN
                    ALTER TABLE public.consultations
                      ADD CONSTRAINT fk_consultations_teacher_user
                      FOREIGN KEY (teacher_user_id) REFERENCES public.users(id);
                  END IF;
                END $$;
                """);
        } catch (Exception e) {
            log.warn("[ConsultationSchema] skip: {}", e.getMessage());
        }
    }
}
