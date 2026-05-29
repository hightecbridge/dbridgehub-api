package com.hiacademy.api.billing;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** 토스페이먼츠 카드사(발급·매입) 코드 → 한글 표시명 */
public final class TossCardCompanyNames {

    private static final Map<String, String> CODE_TO_LABEL = buildMap();

    private TossCardCompanyNames() {}

    private static Map<String, String> buildMap() {
        Map<String, String> m = new HashMap<>();
        m.put("3K", "기업 BC");
        m.put("46", "광주");
        m.put("71", "롯데");
        m.put("30", "산업");
        m.put("31", "BC");
        m.put("51", "삼성");
        m.put("38", "새마을");
        m.put("41", "신한");
        m.put("62", "신협");
        m.put("36", "씨티");
        m.put("33", "우리");
        m.put("W1", "우리");
        m.put("37", "우체국");
        m.put("39", "저축");
        m.put("35", "전북");
        m.put("42", "제주");
        m.put("15", "카카오뱅크");
        m.put("3A", "케이뱅크");
        m.put("24", "토스뱅크");
        m.put("21", "하나");
        m.put("61", "현대");
        m.put("11", "국민");
        m.put("91", "농협");
        m.put("34", "수협");
        m.put("6D", "다이너스");
        m.put("4M", "마스터");
        m.put("3C", "유니온페이");
        m.put("7A", "아메리칸익스프레스");
        m.put("4J", "JCB");
        m.put("4V", "VISA");
        return Map.copyOf(m);
    }

    /**
     * API·DB 카드사명 표시용. 발급사 코드 우선, 없으면 UTF-8 복구한 cardCompany 사용.
     */
    public static String resolveDisplayName(String issuerCode, String cardCompanyRaw) {
        String fromCode = labelFromCode(issuerCode);
        if (fromCode != null) {
            return fromCode;
        }
        return repairUtf8Mojibake(cardCompanyRaw);
    }

    public static String labelFromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String key = code.trim().toUpperCase();
        return CODE_TO_LABEL.get(key);
    }

    /** ISO-8859-1로 잘못 해석된 UTF-8 한글 복구 */
    public static String repairUtf8Mojibake(String value) {
        if (value == null || value.isBlank()) {
            return value == null ? null : value.trim();
        }
        String trimmed = value.trim();
        if (containsHangul(trimmed) && !looksLikeMojibake(trimmed)) {
            return trimmed;
        }
        try {
            String repaired = new String(trimmed.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8).trim();
            if (containsHangul(repaired)) {
                return repaired;
            }
        } catch (Exception ignored) {
            // keep original
        }
        return trimmed;
    }

    private static boolean containsHangul(String s) {
        return s.chars().anyMatch(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HANGUL_SYLLABLES);
    }

    private static boolean looksLikeMojibake(String s) {
        if (containsHangul(s)) {
            return false;
        }
        return s.chars().anyMatch(c -> c >= 0x00C0 && c <= 0x00FF);
    }
}
