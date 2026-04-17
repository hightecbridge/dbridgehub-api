package com.hiacademy.api.billing;

/**
 * 요금제별 재원·휴원 학생(퇴원 제외) 등록 상한.
 * 스탠다드 50 · 프리미엄 100 · 엔터프라이즈 무제한.
 */
public final class BillingPlanLimits {

    private BillingPlanLimits() {}

    /** 미선택·체험 등: 스탠다드와 동일 상한 */
    public static final int DEFAULT_MAX = 50;

    public static int maxStudents(String billingPlanId) {
        if (billingPlanId == null || billingPlanId.isBlank()) {
            return DEFAULT_MAX;
        }
        return switch (billingPlanId.trim().toLowerCase()) {
            case "standard" -> 50;
            case "premium" -> 100;
            case "enterprise" -> Integer.MAX_VALUE;
            default -> DEFAULT_MAX;
        };
    }

    public static boolean isUnlimited(int maxStudents) {
        return maxStudents >= Integer.MAX_VALUE - 1;
    }

    /** subscribe 요청 planId 정규화 */
    public static String normalizePlanId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "standard";
        }
        String s = raw.trim().toLowerCase();
        return switch (s) {
            case "standard", "premium", "enterprise" -> s;
            default -> "standard";
        };
    }
}
