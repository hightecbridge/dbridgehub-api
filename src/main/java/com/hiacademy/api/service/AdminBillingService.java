package com.hiacademy.api.service;

import com.hiacademy.api.dto.request.BillingSmsRequest;
import com.hiacademy.api.dto.response.BillingSummaryResponse;
import com.hiacademy.api.entity.Academy;
import com.hiacademy.api.repository.AcademyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminBillingService {

    private static final Logger log = LoggerFactory.getLogger(AdminBillingService.class);
    private static final int COST_GENERAL = 1;
    private static final int COST_PAYMENT = 2;
    private static final long MONTHLY_KRW = 29000L;

    private final AcademyRepository academyRepo;

    public BillingSummaryResponse getSummary(Long academyId) {
        Academy a = loadAndMigrate(academyId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = a.getTrialEndsAt();
        boolean trialActive = end != null && now.isBefore(end);
        int daysLeft = 0;
        if (end != null && trialActive) {
            daysLeft = (int) Math.max(0, ChronoUnit.DAYS.between(now.toLocalDate(), end.toLocalDate()));
        }
        boolean paymentRequired = !"ACTIVE".equals(a.getBillingStatus())
            && ("PAST_DUE".equals(a.getBillingStatus())
            || ("TRIAL".equals(a.getBillingStatus()) && !trialActive));
        return BillingSummaryResponse.builder()
            .trialEndsAt(end)
            .trialDaysRemaining(daysLeft)
            .trialActive(trialActive)
            .paymentRequired(paymentRequired)
            .billingStatus(a.getBillingStatus())
            .smsPoints(a.getSmsPoints() != null ? a.getSmsPoints() : 0)
            .smsCostGeneral(COST_GENERAL)
            .smsCostPaymentNudge(COST_PAYMENT)
            .monthlyPriceKrw(MONTHLY_KRW)
            .build();
    }

    /** PG 연동 전 스텁: 이용권 활성화 및 체험 종료 후 상태 정리. */
    public BillingSummaryResponse subscribe(Long academyId) {
        Academy a = loadAndMigrate(academyId);
        a.setBillingStatus("ACTIVE");
        a.setTrialEndsAt(null);
        academyRepo.save(a);
        log.info("[Billing] subscribe academyId={}", academyId);
        return getSummary(academyId);
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
        if (changed) academyRepo.save(a);
        return a;
    }
}
