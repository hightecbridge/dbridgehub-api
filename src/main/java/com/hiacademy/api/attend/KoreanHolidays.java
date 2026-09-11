package com.hiacademy.api.attend;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** 대한민국 법정 공휴일 (설·추석·석가는 2020–2035 음력 대조). */
public final class KoreanHolidays {
    private static final Map<Integer, LocalDate> SEOLLAL = lunar(
        2020, 1, 25, 2021, 2, 12, 2022, 2, 1, 2023, 1, 22, 2024, 2, 10,
        2025, 1, 29, 2026, 2, 17, 2027, 2, 6, 2028, 1, 26, 2029, 2, 13,
        2030, 2, 3, 2031, 1, 23, 2032, 2, 11, 2033, 1, 31, 2034, 2, 19, 2035, 2, 8
    );
    private static final Map<Integer, LocalDate> CHUSEOK = lunar(
        2020, 10, 1, 2021, 9, 21, 2022, 9, 10, 2023, 9, 29, 2024, 9, 17,
        2025, 10, 6, 2026, 9, 25, 2027, 9, 15, 2028, 10, 3, 2029, 9, 22,
        2030, 9, 12, 2031, 10, 1, 2032, 9, 19, 2033, 9, 8, 2034, 9, 28, 2035, 9, 17
    );
    private static final Map<Integer, LocalDate> BUDDHA = lunar(
        2020, 4, 30, 2021, 5, 19, 2022, 5, 8, 2023, 5, 27, 2024, 5, 15,
        2025, 5, 5, 2026, 5, 24, 2027, 5, 13, 2028, 5, 2, 2029, 5, 20,
        2030, 5, 9, 2031, 5, 28, 2032, 5, 16, 2033, 5, 5, 2034, 5, 25, 2035, 5, 13
    );

    private KoreanHolidays() {}

    public static boolean isHoliday(LocalDate date) {
        return nameOf(date) != null;
    }

    public static String nameOf(LocalDate date) {
        if (date == null) return null;
        return holidaysOf(date.getYear()).get(date);
    }

    public static Map<LocalDate, String> holidaysOf(int year) {
        Map<LocalDate, String> named = new LinkedHashMap<>();
        put(named, LocalDate.of(year, 1, 1), "신정");
        put(named, LocalDate.of(year, 3, 1), "삼일절");
        put(named, LocalDate.of(year, 5, 5), "어린이날");
        put(named, LocalDate.of(year, 6, 6), "현충일");
        put(named, LocalDate.of(year, 8, 15), "광복절");
        put(named, LocalDate.of(year, 10, 3), "개천절");
        put(named, LocalDate.of(year, 10, 9), "한글날");
        put(named, LocalDate.of(year, 12, 25), "성탄절");

        LocalDate seollal = SEOLLAL.get(year);
        if (seollal != null) {
            put(named, seollal.minusDays(1), "설날 연휴");
            put(named, seollal, "설날");
            put(named, seollal.plusDays(1), "설날 연휴");
        }
        LocalDate chuseok = CHUSEOK.get(year);
        if (chuseok != null) {
            put(named, chuseok.minusDays(1), "추석 연휴");
            put(named, chuseok, "추석");
            put(named, chuseok.plusDays(1), "추석 연휴");
        }
        LocalDate buddha = BUDDHA.get(year);
        if (buddha != null) put(named, buddha, "부처님오신날");

        addSubstitute(named, LocalDate.of(year, 3, 1));
        addSubstitute(named, LocalDate.of(year, 5, 5));
        addSubstitute(named, LocalDate.of(year, 8, 15));
        addSubstitute(named, LocalDate.of(year, 10, 3));
        addSubstitute(named, LocalDate.of(year, 10, 9));
        if (seollal != null) {
            addSubstitute(named, seollal.minusDays(1));
            addSubstitute(named, seollal);
            addSubstitute(named, seollal.plusDays(1));
        }
        if (chuseok != null) {
            addSubstitute(named, chuseok.minusDays(1));
            addSubstitute(named, chuseok);
            addSubstitute(named, chuseok.plusDays(1));
        }
        if (buddha != null && named.containsKey(buddha) && !"부처님오신날".equals(named.get(buddha))) {
            addNextOpen(named, buddha, "대체공휴일");
        }
        return named;
    }

    private static void put(Map<LocalDate, String> named, LocalDate date, String name) {
        named.putIfAbsent(date, name);
    }

    private static void addSubstitute(Map<LocalDate, String> named, LocalDate holiday) {
        DayOfWeek dow = holiday.getDayOfWeek();
        if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) return;
        addNextOpen(named, holiday, "대체공휴일");
    }

    private static void addNextOpen(Map<LocalDate, String> named, LocalDate from, String name) {
        LocalDate cand = from.plusDays(1);
        while (named.containsKey(cand) || cand.getDayOfWeek() == DayOfWeek.SATURDAY || cand.getDayOfWeek() == DayOfWeek.SUNDAY) {
            cand = cand.plusDays(1);
        }
        named.putIfAbsent(cand, name);
    }

    private static Map<Integer, LocalDate> lunar(int... ymd) {
        Map<Integer, LocalDate> map = new HashMap<>();
        for (int i = 0; i + 2 < ymd.length; i += 3) {
            map.put(ymd[i], LocalDate.of(ymd[i], ymd[i + 1], ymd[i + 2]));
        }
        return map;
    }
}
