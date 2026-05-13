package com.hiacademy.api.billing;

/**
 * 요금제별 재원·휴원 학생(퇴원 제외) 등록 상한.
 * 베이직 50 · 스탠다드 100 · 프리미엄 200 · 엔터프라이즈 500.
 */
public final class BillingPlanLimits {

    private BillingPlanLimits() {}

    /** 미선택·체험 등: 베이직과 동일 상한 */
    public static final int DEFAULT_MAX = 50;

    public static int maxStudents(String billingPlanId) {
        if (billingPlanId == null || billingPlanId.isBlank()) {
            return DEFAULT_MAX;
        }
        return switch (billingPlanId.trim().toLowerCase()) {
            case "basic" -> 50;
            case "standard" -> 100;
            case "premium" -> 200;
            case "enterprise" -> 500;
            default -> DEFAULT_MAX;
        };
    }

    public static boolean isUnlimited(int maxStudents) {
        return false;
    }

    /** subscribe 요청 planId 정규화 */
    public static String normalizePlanId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "basic";
        }
        String s = raw.trim().toLowerCase();
        return switch (s) {
            case "basic", "standard", "premium", "enterprise" -> s;
            default -> "basic";
        };
    }
}
