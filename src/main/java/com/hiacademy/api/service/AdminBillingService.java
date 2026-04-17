package com.hiacademy.api.service;

import com.hiacademy.api.billing.BillingPlanLimits;
import com.hiacademy.api.dto.request.BillingPointChargeRequest;
import com.hiacademy.api.dto.request.BillingSmsRequest;
import com.hiacademy.api.dto.request.BillingSubscribeRequest;
import com.hiacademy.api.dto.response.BillingPaymentResponse;
import com.hiacademy.api.dto.response.BillingSummaryResponse;
import com.hiacademy.api.entity.Academy;
import com.hiacademy.api.entity.BillingPaymentLog;
import com.hiacademy.api.entity.StudentStatus;
import com.hiacademy.api.repository.AcademyRepository;
import com.hiacademy.api.repository.BillingPaymentLogRepository;
import com.hiacademy.api.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminBillingService {

    private static final Logger log = LoggerFactory.getLogger(AdminBillingService.class);
    private static final int COST_GENERAL = 1;
    private static final int COST_PAYMENT = 2;
    private static final long MONTHLY_KRW = 29000L;

    private final AcademyRepository academyRepo;
    private final StudentRepository studentRepo;
    private final BillingPaymentLogRepository paymentLogRepo;

    public BillingSummaryResponse getSummary(Long academyId) {
        Academy a = loadAndMigrate(academyId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime trialEnd = a.getTrialEndsAt();
        boolean trialActive = trialEnd != null && now.isBefore(trialEnd);
        int trialDaysLeft = 0;
        if (trialEnd != null && trialActive) {
            trialDaysLeft = (int) Math.max(0, ChronoUnit.DAYS.between(now.toLocalDate(), trialEnd.toLocalDate()));
        }
        LocalDateTime subEnd = a.getSubscriptionEndsAt();
        boolean subscriptionValid = "ACTIVE".equals(a.getBillingStatus())
            && subEnd != null
            && now.isBefore(subEnd);
        int subscriptionDaysLeft = 0;
        if (subscriptionValid && subEnd != null) {
            subscriptionDaysLeft = (int) Math.max(0, ChronoUnit.DAYS.between(now.toLocalDate(), subEnd.toLocalDate()));
        }
        boolean paymentRequired = !"ACTIVE".equals(a.getBillingStatus())
            && ("PAST_DUE".equals(a.getBillingStatus())
            || ("TRIAL".equals(a.getBillingStatus()) && !trialActive));
        long studentCount = studentRepo.countByAcademyIdExcludingWithdrawn(academyId, StudentStatus.퇴원);
        int maxSt = BillingPlanLimits.maxStudents(a.getBillingPlanId());
        int studentLimit = BillingPlanLimits.isUnlimited(maxSt) ? -1 : maxSt;
        return BillingSummaryResponse.builder()
            .trialEndsAt(trialEnd)
            .trialDaysRemaining(trialDaysLeft)
            .trialActive(trialActive)
            .subscriptionEndsAt(subEnd)
            .subscriptionDaysRemaining(subscriptionDaysLeft)
            .paymentRequired(paymentRequired)
            .billingStatus(a.getBillingStatus())
            .smsPoints(a.getSmsPoints() != null ? a.getSmsPoints() : 0)
            .smsCostGeneral(COST_GENERAL)
            .smsCostPaymentNudge(COST_PAYMENT)
            .monthlyPriceKrw(MONTHLY_KRW)
            .billingPlanId(a.getBillingPlanId())
            .studentCount(studentCount)
            .studentLimit(studentLimit)
            .build();
    }

    /**
     * 결제 완료 반영: 선택한 결제 주기만큼 이용 만료일을 연장합니다.
     * 체험 중이면 체험 종료 시점부터, 이미 유료 기간이 남아 있으면 그 만료일부터 이어서 연장합니다.
     */
    public BillingSummaryResponse subscribe(Long academyId, BillingSubscribeRequest req) {
        Academy a = loadAndMigrate(academyId);
        int months = billingMonthsFromRequest(req);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime anchor = now;

        if ("TRIAL".equals(a.getBillingStatus()) && a.getTrialEndsAt() != null && now.isBefore(a.getTrialEndsAt())) {
            anchor = a.getTrialEndsAt();
        } else if (a.getSubscriptionEndsAt() != null && now.isBefore(a.getSubscriptionEndsAt())) {
            anchor = a.getSubscriptionEndsAt();
        }

        LocalDateTime newEnd = anchor.plusMonths(months);
        a.setBillingStatus("ACTIVE");
        a.setTrialEndsAt(null);
        a.setSubscriptionEndsAt(newEnd);
        if (req != null && req.getPlanId() != null && !req.getPlanId().isBlank()) {
            a.setBillingPlanId(BillingPlanLimits.normalizePlanId(req.getPlanId()));
        }
        academyRepo.save(a);
        appendSubscriptionLog(a, months, req);
        log.info("[Billing] subscribe academyId={} months={} plan={} subscriptionEndsAt={}", academyId, months, a.getBillingPlanId(), newEnd);
        return getSummary(academyId);
    }

    /** 포인트 충전(결제 완료 후 호출). */
    public BillingSummaryResponse chargePoints(Long academyId, BillingPointChargeRequest req) {
        int pts = req.getPoints();
        if (pts != 10_000 && pts != 20_000 && pts != 30_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "충전 금액은 10,000 / 20,000 / 30,000원만 가능합니다.");
        }
        Academy a = loadAndMigrate(academyId);
        int cur = a.getSmsPoints() != null ? a.getSmsPoints() : 0;
        a.setSmsPoints(cur + pts);
        academyRepo.save(a);
        String summary = String.format(Locale.KOREA, "포인트 %,dP 충전 (결제 %,d원)", pts, pts);
        paymentLogRepo.save(BillingPaymentLog.builder()
            .academy(a)
            .paymentType("POINT_CHARGE")
            .amountKrw(pts)
            .orderId(req.getOrderId())
            .summary(summary)
            .build());
        log.info("[Billing] chargePoints academyId={} points={}", academyId, pts);
        return getSummary(academyId);
    }

    public List<BillingPaymentResponse> listPayments(Long academyId) {
        loadAndMigrate(academyId);
        return paymentLogRepo.findByAcademy_IdOrderByCreatedAtDesc(academyId).stream()
            .map(log -> BillingPaymentResponse.builder()
                .id(log.getId())
                .paymentType(log.getPaymentType())
                .amountKrw(log.getAmountKrw())
                .orderId(log.getOrderId())
                .summary(log.getSummary())
                .paidAt(log.getCreatedAt())
                .build())
            .toList();
    }

    private void appendSubscriptionLog(Academy a, int months, BillingSubscribeRequest req) {
        long amt = req != null && req.getPaidAmountKrw() != null ? req.getPaidAmountKrw() : 0L;
        String ord = req != null ? req.getOrderId() : null;
        String plan = planLabel(a.getBillingPlanId());
        String cycle = months == 12 ? "연간" : "월간";
        String end = a.getSubscriptionEndsAt() != null
            ? a.getSubscriptionEndsAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            : "-";
        String summary;
        if (amt > 0) {
            summary = String.format(Locale.KOREA, "결제 %,d원 · %s %s 구독 · 이용 만료 %s", amt, plan, cycle, end);
        } else {
            summary = String.format("%s %s 구독 · 이용 만료 %s", plan, cycle, end);
        }
        paymentLogRepo.save(BillingPaymentLog.builder()
            .academy(a)
            .paymentType("SUBSCRIPTION")
            .amountKrw(amt)
            .orderId(ord)
            .summary(summary)
            .build());
    }

    private static String planLabel(String planId) {
        if (planId == null || planId.isBlank()) {
            return "스탠다드";
        }
        return switch (planId.trim().toLowerCase()) {
            case "starter" -> "스타터";
            case "standard" -> "스탠다드";
            case "premium" -> "프리미엄";
            case "enterprise" -> "엔터프라이즈";
            default -> planId;
        };
    }

    private static int billingMonthsFromRequest(BillingSubscribeRequest req) {
        if (req == null || req.getBillingCycle() == null || req.getBillingCycle().isBlank()) {
            return 1;
        }
        return "YEARLY".equalsIgnoreCase(req.getBillingCycle().trim()) ? 12 : 1;
    }

    public BillingSummaryResponse sendSms(Long academyId, BillingSmsRequest req) {
        Academy a = loadAndMigrate(academyId);
        int cost = switch (req.getType().trim().toUpperCase()) {
            case "GENERAL" -> COST_GENERAL;
            case "PAYMENT_NUDGE" -> COST_PAYMENT;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type은 GENERAL 또는 PAYMENT_NUDGE 이어야 합니다.");
        };
        int pts = a.getSmsPoints() != null ? a.getSmsPoints() : 0;
        if (pts < cost) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "포인트가 부족합니다. 충전 후 이용해 주세요.");
        }
        a.setSmsPoints(pts - cost);
        academyRepo.save(a);
        log.info("[Billing] sms type={} academyId={} cost={} remaining={}", req.getType(), academyId, cost, a.getSmsPoints());
        // 실제 SMS 연동 시 여기서 발송
        return getSummary(academyId);
    }

    private Academy loadAndMigrate(Long academyId) {
        Academy a = academyRepo.findById(academyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "학원을 찾을 수 없습니다."));
        boolean changed = false;
        if (a.getTrialEndsAt() == null && a.getCreatedAt() != null) {
            a.setTrialEndsAt(a.getCreatedAt().plusDays(30));
            changed = true;
        }
        if (a.getSmsPoints() == null) {
            a.setSmsPoints(300);
            changed = true;
        }
        if (a.getBillingStatus() == null || a.getBillingStatus().isBlank()) {
            a.setBillingStatus("TRIAL");
            changed = true;
        }
        LocalDateTime now = LocalDateTime.now();
        if ("TRIAL".equals(a.getBillingStatus()) && a.getTrialEndsAt() != null && !now.isBefore(a.getTrialEndsAt())) {
            a.setBillingStatus("PAST_DUE");
            changed = true;
        }
        if ("ACTIVE".equals(a.getBillingStatus())
            && a.getSubscriptionEndsAt() != null
            && !now.isBefore(a.getSubscriptionEndsAt())) {
            a.setBillingStatus("PAST_DUE");
            changed = true;
        }
        if (changed) academyRepo.save(a);
        return a;
    }
}
