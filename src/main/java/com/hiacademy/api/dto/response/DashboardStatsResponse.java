package com.hiacademy.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder
public class DashboardStatsResponse {
    private int year;
    private int yearEnrolled;
    private int yearWithdrawn;
    private long yearRevenue;
    private long yearUnpaid;
    private List<MonthItem> months;

    @Data @Builder
    public static class MonthItem {
        private int month;
        private int enrolled;
        private int withdrawn;
        private long revenue;
        private long unpaid;
    }
}
