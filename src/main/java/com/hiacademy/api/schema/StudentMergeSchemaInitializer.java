package com.hiacademy.api.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * parents 테이블 데이터를 students 로 복사하고 academy_id 를 채웁니다.
 */
@Component
public class StudentMergeSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StudentMergeSchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public StudentMergeSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureStudentColumns();
            migratePushTokens();
            copyParentFieldsToStudents();
            backfillAcademyFromParent();
        } catch (Exception e) {
            log.warn("[StudentMerge] schema migration skipped: {}", e.getMessage(), e);
        }
    }

    private void ensureStudentColumns() {
        jdbc.execute("""
            ALTER TABLE students ADD COLUMN IF NOT EXISTS parent_name VARCHAR(255);
            ALTER TABLE students ADD COLUMN IF NOT EXISTS parent_phone VARCHAR(64);
            ALTER TABLE students ADD COLUMN IF NOT EXISTS login_phone VARCHAR(64);
            ALTER TABLE students ADD COLUMN IF NOT EXISTS login_password VARCHAR(255);
            ALTER TABLE students ADD COLUMN IF NOT EXISTS badge_color VARCHAR(32);
            ALTER TABLE students ADD COLUMN IF NOT EXISTS badge_text_color VARCHAR(32);
            ALTER TABLE students ADD COLUMN IF NOT EXISTS kakao_linked BOOLEAN DEFAULT false;
            ALTER TABLE students ADD COLUMN IF NOT EXISTS academy_id BIGINT REFERENCES academies(id);
            ALTER TABLE students ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
            ALTER TABLE students ALTER COLUMN parent_id DROP NOT NULL;
            """);
    }

    private void migratePushTokens() {
        jdbc.execute("""
            ALTER TABLE parent_push_tokens ADD COLUMN IF NOT EXISTS student_id BIGINT REFERENCES students(id);
            """);
        int updated = jdbc.update("""
            UPDATE parent_push_tokens t
            SET student_id = s.id
            FROM students s
            WHERE t.student_id IS NULL
              AND t.parent_id IS NOT NULL
              AND s.parent_id = t.parent_id
              AND s.id = (
                SELECT MIN(s2.id) FROM students s2 WHERE s2.parent_id = t.parent_id
              )
            """);
        if (updated > 0) {
            log.info("[StudentMerge] linked {} push token row(s) to students", updated);
        }
    }

    private void copyParentFieldsToStudents() {
        int updated = jdbc.update("""
            UPDATE students s
            SET parent_name = COALESCE(NULLIF(TRIM(s.parent_name), ''), p.name),
                parent_phone = COALESCE(NULLIF(TRIM(s.parent_phone), ''), p.phone),
                login_phone = COALESCE(NULLIF(TRIM(s.login_phone), ''), p.login_phone, p.phone),
                login_password = COALESCE(s.login_password, p.login_password),
                badge_color = COALESCE(s.badge_color, p.badge_color),
                badge_text_color = COALESCE(s.badge_text_color, p.badge_text_color),
                kakao_linked = COALESCE(s.kakao_linked, p.kakao_linked, false),
                academy_id = COALESCE(s.academy_id, p.academy_id),
                created_at = COALESCE(s.created_at, p.created_at, NOW())
            FROM parents p
            WHERE s.parent_id = p.id
              AND (
                s.parent_name IS NULL OR TRIM(s.parent_name) = ''
                OR s.parent_phone IS NULL OR TRIM(s.parent_phone) = ''
                OR s.academy_id IS NULL
              )
            """);
        if (updated > 0) {
            log.info("[StudentMerge] copied parent fields to {} student row(s)", updated);
        }
    }

    private void backfillAcademyFromParent() {
        int updated = jdbc.update("""
            UPDATE students s
            SET academy_id = p.academy_id
            FROM parents p
            WHERE s.parent_id = p.id AND s.academy_id IS NULL
            """);
        if (updated > 0) {
            log.info("[StudentMerge] backfilled academy_id on {} student row(s)", updated);
        }
    }
}
