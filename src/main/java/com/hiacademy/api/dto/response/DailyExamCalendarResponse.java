package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DailyExamCalendarResponse {
    private int year;
    private int month;
    private List<DayCount> days;

    @Data
    @Builder
    public static class DayCount {
        private String date;
        private int count;
    }
}
