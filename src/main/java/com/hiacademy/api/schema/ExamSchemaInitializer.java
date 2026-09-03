package com.hiacademy.api.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ExamSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ExamSchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public ExamSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS public.exams (
                  id BIGSERIAL PRIMARY KEY,
                  title VARCHAR(255) NOT NULL,
                  exam_date DATE NOT NULL,
                  subject VARCHAR(255),
                  max_score INT NOT NULL DEFAULT 100,
                  classroom_id BIGINT NOT NULL,
                  academy_id BIGINT NOT NULL,
                  created_at TIMESTAMP
                )
                """);
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS public.exam_scores (
                  id BIGSERIAL PRIMARY KEY,
                  exam_id BIGINT NOT NULL,
                  student_id BIGINT NOT NULL,
                  score DOUBLE PRECISION,
                  comment TEXT,
                  updated_at TIMESTAMP
                )
                """);
            jdbc.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_exams_classroom') THEN
                    ALTER TABLE public.exams
                      ADD CONSTRAINT fk_exams_classroom
                      FOREIGN KEY (classroom_id) REFERENCES public.classrooms(id) ON DELETE CASCADE;
                  END IF;
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_exams_academy') THEN
                    ALTER TABLE public.exams
                      ADD CONSTRAINT fk_exams_academy
                      FOREIGN KEY (academy_id) REFERENCES public.academies(id) ON DELETE CASCADE;
                  END IF;
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_exam_scores_exam') THEN
                    ALTER TABLE public.exam_scores
                      ADD CONSTRAINT fk_exam_scores_exam
                      FOREIGN KEY (exam_id) REFERENCES public.exams(id) ON DELETE CASCADE;
                  END IF;
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_exam_scores_student') THEN
                    ALTER TABLE public.exam_scores
                      ADD CONSTRAINT fk_exam_scores_student
                      FOREIGN KEY (student_id) REFERENCES public.students(id) ON DELETE CASCADE;
                  END IF;
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_exam_scores_exam_student') THEN
                    ALTER TABLE public.exam_scores
                      ADD CONSTRAINT uk_exam_scores_exam_student UNIQUE (exam_id, student_id);
                  END IF;
                END $$;
                """);
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_exams_classroom_id ON public.exams(classroom_id)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_exam_scores_exam_id ON public.exam_scores(exam_id)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_exam_scores_student_id ON public.exam_scores(student_id)");
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS public.exam_sections (
                  id BIGSERIAL PRIMARY KEY,
                  exam_id BIGINT NOT NULL,
                  name VARCHAR(80) NOT NULL,
                  max_score INT NOT NULL DEFAULT 100,
                  sort_order INT NOT NULL DEFAULT 0
                )
                """);
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS public.exam_score_sections (
                  id BIGSERIAL PRIMARY KEY,
                  exam_score_id BIGINT NOT NULL,
                  exam_section_id BIGINT NOT NULL,
                  score DOUBLE PRECISION,
                  percent DOUBLE PRECISION
                )
                """);
            jdbc.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_exam_sections_exam') THEN
                    ALTER TABLE public.exam_sections
                      ADD CONSTRAINT fk_exam_sections_exam
                      FOREIGN KEY (exam_id) REFERENCES public.exams(id) ON DELETE CASCADE;
                  END IF;
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_exam_score_sections_score') THEN
                    ALTER TABLE public.exam_score_sections
                      ADD CONSTRAINT fk_exam_score_sections_score
                      FOREIGN KEY (exam_score_id) REFERENCES public.exam_scores(id) ON DELETE CASCADE;
                  END IF;
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_exam_score_sections_section') THEN
                    ALTER TABLE public.exam_score_sections
                      ADD CONSTRAINT fk_exam_score_sections_section
                      FOREIGN KEY (exam_section_id) REFERENCES public.exam_sections(id) ON DELETE CASCADE;
                  END IF;
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_exam_score_sections_score_section') THEN
                    ALTER TABLE public.exam_score_sections
                      ADD CONSTRAINT uk_exam_score_sections_score_section UNIQUE (exam_score_id, exam_section_id);
                  END IF;
                END $$;
                """);
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_exam_sections_exam_id ON public.exam_sections(exam_id)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_exam_score_sections_score_id ON public.exam_score_sections(exam_score_id)");
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS public.exam_classrooms (
                  exam_id BIGINT NOT NULL,
                  classroom_id BIGINT NOT NULL,
                  PRIMARY KEY (exam_id, classroom_id)
                )
                """);
            jdbc.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_exam_classrooms_exam') THEN
                    ALTER TABLE public.exam_classrooms
                      ADD CONSTRAINT fk_exam_classrooms_exam
                      FOREIGN KEY (exam_id) REFERENCES public.exams(id) ON DELETE CASCADE;
                  END IF;
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_exam_classrooms_classroom') THEN
                    ALTER TABLE public.exam_classrooms
                      ADD CONSTRAINT fk_exam_classrooms_classroom
                      FOREIGN KEY (classroom_id) REFERENCES public.classrooms(id) ON DELETE CASCADE;
                  END IF;
                END $$;
                """);
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_exam_classrooms_classroom_id ON public.exam_classrooms(classroom_id)");
            jdbc.execute("""
                INSERT INTO public.exam_classrooms (exam_id, classroom_id)
                SELECT e.id, e.classroom_id FROM public.exams e
                WHERE e.classroom_id IS NOT NULL
                  AND NOT EXISTS (
                    SELECT 1 FROM public.exam_classrooms x
                    WHERE x.exam_id = e.id AND x.classroom_id = e.classroom_id
                  )
                """);
            try {
                jdbc.execute("ALTER TABLE public.exams ALTER COLUMN classroom_id DROP NOT NULL");
            } catch (Exception ignored) {
                // already nullable
            }
            jdbc.execute("""
                DO $$
                BEGIN
                  IF EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'fk_exams_classroom'
                  ) THEN
                    ALTER TABLE public.exams DROP CONSTRAINT fk_exams_classroom;
                  END IF;
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_exams_classroom') THEN
                    ALTER TABLE public.exams
                      ADD CONSTRAINT fk_exams_classroom
                      FOREIGN KEY (classroom_id) REFERENCES public.classrooms(id) ON DELETE SET NULL;
                  END IF;
                END $$;
                """);
            try {
                jdbc.execute("ALTER TABLE public.exams ADD COLUMN IF NOT EXISTS kind VARCHAR(16) NOT NULL DEFAULT 'CLASS'");
            } catch (Exception ignored) {}
            try {
                jdbc.execute("ALTER TABLE public.exams ADD COLUMN IF NOT EXISTS closed BOOLEAN NOT NULL DEFAULT FALSE");
            } catch (Exception ignored) {}
            try {
                jdbc.execute("ALTER TABLE public.exam_sections ADD COLUMN IF NOT EXISTS weight INT NOT NULL DEFAULT 0");
            } catch (Exception ignored) {}
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS public.exam_classroom_inputs (
                  id BIGSERIAL PRIMARY KEY,
                  exam_id BIGINT NOT NULL,
                  classroom_id BIGINT NOT NULL,
                  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
                  updated_at TIMESTAMP
                )
                """);
            jdbc.execute("""
                DO $$
                BEGIN
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_exam_classroom_inputs_exam') THEN
                    ALTER TABLE public.exam_classroom_inputs
                      ADD CONSTRAINT fk_exam_classroom_inputs_exam
                      FOREIGN KEY (exam_id) REFERENCES public.exams(id) ON DELETE CASCADE;
                  END IF;
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_exam_classroom_inputs_classroom') THEN
                    ALTER TABLE public.exam_classroom_inputs
                      ADD CONSTRAINT fk_exam_classroom_inputs_classroom
                      FOREIGN KEY (classroom_id) REFERENCES public.classrooms(id) ON DELETE CASCADE;
                  END IF;
                  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_exam_classroom_inputs_exam_class') THEN
                    ALTER TABLE public.exam_classroom_inputs
                      ADD CONSTRAINT uk_exam_classroom_inputs_exam_class UNIQUE (exam_id, classroom_id);
                  END IF;
                END $$;
                """);
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_exam_classroom_inputs_exam_id ON public.exam_classroom_inputs(exam_id)");
        } catch (Exception e) {
            log.warn("[ExamSchema] skip: {}", e.getMessage());
        }
    }
}
