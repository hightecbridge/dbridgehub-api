package com.hiacademy.api.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * parents.login_phone 전역 UNIQUE 제약을 학원별(academy_id + login_phone) UNIQUE 로 전환합니다.
 */
@Component
public class ParentSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ParentSchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public ParentSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            normalizePhoneColumns();
            dropGlobalLoginPhoneUnique();
            ensureAcademyScopedLoginPhoneUnique();
        } catch (Exception e) {
            log.warn("[ParentSchema] 스키마 초기화 실패: {}", e.getMessage(), e);
        }
    }

    private void normalizePhoneColumns() {
        int updated = jdbc.update(
            """
            UPDATE parents
            SET phone = regexp_replace(phone, '[^0-9]', '', 'g'),
                login_phone = regexp_replace(login_phone, '[^0-9]', '', 'g')
            WHERE phone ~ '[^0-9]' OR login_phone ~ '[^0-9]'
            """
        );
        if (updated > 0) {
            log.info("[ParentSchema] normalized phone/login_phone on {} parent row(s)", updated);
        }
    }

    private void dropGlobalLoginPhoneUnique() {
        Integer exists = jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_constraint
            WHERE conrelid = 'public.parents'::regclass
              AND contype = 'u'
              AND pg_get_constraintdef(oid) LIKE '%(login_phone)%'
              AND pg_get_constraintdef(oid) NOT LIKE '%academy_id%'
            """,
            Integer.class
        );
        if (exists != null && exists > 0) {
            jdbc.execute(
                """
                DO $$
                DECLARE cname text;
                BEGIN
                  SELECT conname INTO cname
                  FROM pg_constraint
                  WHERE conrelid = 'public.parents'::regclass
                    AND contype = 'u'
                    AND pg_get_constraintdef(oid) LIKE '%(login_phone)%'
                    AND pg_get_constraintdef(oid) NOT LIKE '%academy_id%'
                  LIMIT 1;
                  IF cname IS NOT NULL THEN
                    EXECUTE format('ALTER TABLE public.parents DROP CONSTRAINT %I', cname);
                  END IF;
                END $$;
                """
            );
            log.info("[ParentSchema] dropped global unique constraint on parents.login_phone");
        }
    }

    private void ensureAcademyScopedLoginPhoneUnique() {
        Integer exists = jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM pg_constraint
            WHERE conrelid = 'public.parents'::regclass
              AND conname = 'uk_parents_academy_login_phone'
            """,
            Integer.class
        );
        if (exists == null || exists == 0) {
            jdbc.execute(
                """
                ALTER TABLE public.parents
                ADD CONSTRAINT uk_parents_academy_login_phone
                UNIQUE (academy_id, login_phone)
                """
            );
            log.info("[ParentSchema] added uk_parents_academy_login_phone");
        }
    }
}
