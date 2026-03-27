package com.hiacademy.api.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 테스트/개발 환경에서 notices 테이블 스키마가 JPA 매핑과 불일치할 때
 * (예: image_url varchar(255), image_data 컬럼 없음) 목록/저장이 0건이 되는 문제를 방지.
 */
@Component
public class NoticeSchemaInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(NoticeSchemaInitializer.class);
    private final JdbcTemplate jdbc;

    public NoticeSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            ensureImageColumns();
        } catch (Exception e) {
            // 스키마 수정이 실패하면, 이후 API가 실제 예외를 던지므로 여기서는 로그만 남김
            log.warn("[NoticeSchema] 스키마 초기화 실패: {}", e.getMessage(), e);
        }
    }

    private void ensureImageColumns() {
        // 1) image_data 컬럼 존재 여부
        int imageDataExists = jdbc.queryForObject(
            """
            SELECT COUNT(*) 
            FROM information_schema.columns 
            WHERE table_schema = 'public'
              AND table_name   = 'notices'
              AND column_name  = 'image_data'
            """,
            Integer.class
        );

        if (imageDataExists == 0) {
            log.info("[NoticeSchema] adding column notices.image_data (TEXT)");
            jdbc.execute("ALTER TABLE public.notices ADD COLUMN image_data TEXT");
        }

        // 2) image_url 컬럼 타입이 TEXT인지 확인 (varchar(255)면 TEXT로 변경)
        Map<String, Object> row = jdbc.queryForMap(
            """
            SELECT data_type, character_maximum_length
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name   = 'notices'
              AND column_name  = 'image_url'
            """
        );

        String dataType = (String) row.get("data_type");
        Object maxLenObj = row.get("character_maximum_length");
        Integer maxLen = maxLenObj == null ? null : ((Number) maxLenObj).intValue();

        boolean looksLikeVarchar = maxLen != null && maxLen > 0;
        boolean isText = "text".equalsIgnoreCase(dataType);

        if (!isText && looksLikeVarchar) {
            log.info("[NoticeSchema] altering notices.image_url to TEXT (current data_type={}, maxLen={})", dataType, maxLen);
            jdbc.execute("ALTER TABLE public.notices ALTER COLUMN image_url TYPE TEXT");
        }
    }
}

