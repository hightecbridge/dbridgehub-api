package com.hiacademy.api.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * classroom_id 가 비어 있는 학생을 출석·숙제 시트에 기록된 반(classroom)으로 연결합니다.
 * 기존 반별 출석/숙제 데이터와 학생 roster 를 맞추기 위한 1회성 보정입니다.
 */
@Component
public class StudentClassroomBackfillInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(StudentClassroomBackfillInitializer.class);
    private final JdbcTemplate jdbc;

    public StudentClassroomBackfillInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int fromAttend = backfillFromAttendSheets();
            int fromHomework = backfillFromHomeworkSheets();
            if (fromAttend > 0 || fromHomework > 0) {
                log.info("[StudentClassroom] backfilled classroom_id — attend: {}, homework: {}",
                    fromAttend, fromHomework);
            }
        } catch (Exception e) {
            log.warn("[StudentClassroom] classroom backfill skipped: {}", e.getMessage(), e);
        }
    }

    private int backfillFromAttendSheets() {
        return jdbc.update("""
            UPDATE students s
            SET classroom_id = picked.classroom_id
            FROM (
                SELECT DISTINCT ON (ar.student_id)
                    ar.student_id,
                    ash.classroom_id
                FROM attend_records ar
                INNER JOIN attend_sheets ash ON ash.id = ar.sheet_id
                INNER JOIN students st ON st.id = ar.student_id
                WHERE st.classroom_id IS NULL
                ORDER BY ar.student_id, ash.attend_date DESC, ar.id DESC
            ) picked
            WHERE s.id = picked.student_id
              AND s.classroom_id IS NULL
            """);
    }

    private int backfillFromHomeworkSheets() {
        return jdbc.update("""
            UPDATE students s
            SET classroom_id = picked.classroom_id
            FROM (
                SELECT DISTINCT ON (hr.student_id)
                    hr.student_id,
                    hs.classroom_id
                FROM homework_records hr
                INNER JOIN homework_sheets hs ON hs.id = hr.sheet_id
                INNER JOIN students st ON st.id = hr.student_id
                WHERE st.classroom_id IS NULL
                ORDER BY hr.student_id, hs.homework_date DESC, hr.id DESC
            ) picked
            WHERE s.id = picked.student_id
              AND s.classroom_id IS NULL
            """);
    }
}
