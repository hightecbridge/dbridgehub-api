package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class ConsultStatsResponse {
    private int year;
    private int month;
    private int totalCount;
    private int enrolledCount;
    private int prospectCount;
    private List<CountItem> byTeacher;
    private List<CountItem> byStudent;
    private List<DayItem> byDate;

    @Data @Builder
    public static class CountItem {
        private Long id;
        private String name;
        private int count;
    }

    @Data @Builder
    public static class DayItem {
        private String date;
        private int count;
        private List<String> names;
    }
}
