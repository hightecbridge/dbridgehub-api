package com.hiacademy.api.service;

import com.hiacademy.api.billing.BillingPlanLimits;
import com.hiacademy.api.billing.TossCardCompanyNames;
import com.hiacademy.api.dto.request.BillingAutoSubscribeRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Base64;

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
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${toss.payments.secret-key:${TOSS_PAYMENTS_SECRET_KEY:}}")
    private String tossSecretKey;

    public BillingSummaryResponse getSummary(Long academyId) {
        Academy a = loadAndMigrate(academyId);
        maybeRepairStoredCardCompany(a);
        SmsCosts smsCosts = loadSmsCosts();
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
            .smsCostGeneral(smsCosts.sms())
            .smsCostKakaoAlimtalk(smsCosts.kakaoAlimtalk())
            .smsCostSms(smsCosts.sms())
            .smsCostLms(smsCosts.lms())
            .smsCostMms(smsCosts.mms())
            .smsCostPaymentNudge(smsCosts.paymentSms())
            .monthlyPriceKrw(MONTHLY_KRW)
            .billingPlanId(a.getBillingPlanId())
            .studentCount(studentCount)
            .studentLimit(studentLimit)
            .autoBillingEnabled(Boolean.TRUE.equals(a.getAutoBillingEnabled()))
            .billingKeyIssuedAt(a.getBillingKeyIssuedAt())
            .billingCardLast4(a.getBillingCardLast4())
            .billingCardCompany(resolveStoredCardCompany(a))
            .billingCardExpMonth(a.getBillingCardExpMonth())
            .billingCardExpYear(a.getBillingCardExpYear())
            .build();
    }

    /**
     * 결제 완료 반영: 선택한 결제 주기만큼 이용 만료일을 연장합니다.
     * 체험 중이면 체험 종료 시점부터, 이미 유료 기간이 남아 있으면 그 만료일부터 이어서 연장합니다.
     */
    public BillingSummaryResponse subscribe(Long academyId, BillingSubscribeRequest req) {
        Academy a = loadAndMigrate(academyId);
        if (req != null && req.getPlanId() != null && !req.getPlanId().isBlank()) {
            validatePlanChange(a, BillingPlanLimits.normalizePlanId(req.getPlanId()));
        }
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

    /** 카드 등록 성공 후 빌링키를 발급/저장하고 최초 1회 청구 + 구독 반영 */
    public BillingSummaryResponse registerAndChargeAutoBilling(Long academyId, BillingAutoSubscribeRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자동결제 요청값이 필요합니다.");
        }
        if (req.getCustomerKey() == null || req.getCustomerKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerKey가 필요합니다.");
        }
        if (req.getCardNumber() == null || req.getCardNumber().isBlank()
            || req.getCardExpirationYear() == null || req.getCardExpirationYear().isBlank()
            || req.getCardExpirationMonth() == null || req.getCardExpirationMonth().isBlank()
            || req.getCustomerIdentityNumber() == null || req.getCustomerIdentityNumber().isBlank()
            || req.getCardPassword() == null || req.getCardPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카드 정보(cardNumber, cardExpirationYear, cardExpirationMonth, customerIdentityNumber, cardPassword)가 필요합니다.");
        }
        Academy a = loadAndMigrate(academyId);
        if (req.getPlanId() != null && !req.getPlanId().isBlank()) {
            validatePlanChange(a, BillingPlanLimits.normalizePlanId(req.getPlanId()));
        }
        TossBillingKeyIssueResult issue = issueBillingKey(req);
        a.setTossCustomerKey(req.getCustomerKey());
        a.setTossAuthKey(null);
        a.setTossBillingKey(issue.billingKey());
        a.setAutoBillingEnabled(true);
        a.setBillingKeyIssuedAt(LocalDateTime.now());
        applyBillingCardMeta(a, issue.cardMeta());
        if (req.getPlanId() != null && !req.getPlanId().isBlank()) {
            a.setBillingPlanId(BillingPlanLimits.normalizePlanId(req.getPlanId()));
        }
        academyRepo.save(a);

        long amount = req.getPaidAmountKrw() != null && req.getPaidAmountKrw() > 0
            ? req.getPaidAmountKrw()
            : monthlyAmountForPlan(a.getBillingPlanId());
        String orderId = req.getOrderId() != null && !req.getOrderId().isBlank()
            ? req.getOrderId()
            : "SUB-REG-" + academyId + "-" + System.currentTimeMillis();
        chargeWithBillingKey(issue.billingKey(), req.getCustomerKey(), amount, orderId, "하이아카데미 정기결제");

        BillingSubscribeRequest subscribeReq = new BillingSubscribeRequest();
        subscribeReq.setPlanId(a.getBillingPlanId());
        subscribeReq.setBillingCycle("MONTHLY");
        subscribeReq.setOrderId(orderId);
        subscribeReq.setPaidAmountKrw(amount);
        return subscribe(academyId, subscribeReq);
    }

    /**
     * 등록된 빌링키로 요금제 변경(업그레이드·다운그레이드, 카드 재입력 없음).
     * 다운그레이드는 등록 학생 수가 변경 요금제 상한 이하일 때만 허용합니다.
     */
    public BillingSummaryResponse changeBillingPlan(Long academyId, BillingSubscribeRequest req) {
        if (req == null || req.getPlanId() == null || req.getPlanId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요금제(planId)가 필요합니다.");
        }
        Academy a = loadAndMigrate(academyId);
        if (!Boolean.TRUE.equals(a.getAutoBillingEnabled()) || a.getBillingKeyIssuedAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "정기결제가 등록되어 있지 않습니다. 카드 정보를 등록해 주세요.");
        }
        if (a.getTossBillingKey() == null || a.getTossBillingKey().isBlank()
            || a.getTossCustomerKey() == null || a.getTossCustomerKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "등록된 결제 수단이 없습니다. 카드 정보 변경 후 다시 시도해 주세요.");
        }
        String newPlan = BillingPlanLimits.normalizePlanId(req.getPlanId());
        validatePlanChange(a, newPlan);
        String current = BillingPlanLimits.normalizePlanId(a.getBillingPlanId());
        if (current.equals(newPlan)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재와 동일한 요금제입니다.");
        }
        long amount = req.getPaidAmountKrw() != null && req.getPaidAmountKrw() > 0
            ? req.getPaidAmountKrw()
            : monthlyAmountForPlan(newPlan);
        String orderId = req.getOrderId() != null && !req.getOrderId().isBlank()
            ? req.getOrderId()
            : (BillingPlanLimits.isDowngrade(current, newPlan) ? "DWN-" : "UPG-") + academyId + "-" + System.currentTimeMillis();
        String chargeLabel = BillingPlanLimits.isDowngrade(current, newPlan)
            ? "하이아카데미 요금제 다운그레이드"
            : "하이아카데미 요금제 업그레이드";
        chargeWithBillingKey(a.getTossBillingKey(), a.getTossCustomerKey(), amount, orderId, chargeLabel);
        a.setBillingPlanId(newPlan);
        academyRepo.save(a);
        BillingSubscribeRequest subscribeReq = new BillingSubscribeRequest();
        subscribeReq.setPlanId(newPlan);
        subscribeReq.setBillingCycle(req.getBillingCycle() != null ? req.getBillingCycle() : "MONTHLY");
        subscribeReq.setOrderId(orderId);
        subscribeReq.setPaidAmountKrw(amount);
        return subscribe(academyId, subscribeReq);
    }

    /** 정기결제 등록 후 카드 정보만 변경(빌링키 재발급, 즉시 청구 없음) */
    public BillingSummaryResponse changeBillingCard(Long academyId, BillingAutoSubscribeRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청값이 필요합니다.");
        }
        if (req.getCustomerKey() == null || req.getCustomerKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customerKey가 필요합니다.");
        }
        if (req.getCardNumber() == null || req.getCardNumber().isBlank()
            || req.getCardExpirationYear() == null || req.getCardExpirationYear().isBlank()
            || req.getCardExpirationMonth() == null || req.getCardExpirationMonth().isBlank()
            || req.getCustomerIdentityNumber() == null || req.getCustomerIdentityNumber().isBlank()
            || req.getCardPassword() == null || req.getCardPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "카드 정보가 필요합니다.");
        }
        Academy a = loadAndMigrate(academyId);
        if (!Boolean.TRUE.equals(a.getAutoBillingEnabled()) || a.getBillingKeyIssuedAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "정기결제가 등록되어 있지 않습니다.");
        }
        TossBillingKeyIssueResult issue = issueBillingKey(req);
        a.setTossCustomerKey(req.getCustomerKey());
        a.setTossAuthKey(null);
        a.setTossBillingKey(issue.billingKey());
        a.setBillingKeyIssuedAt(LocalDateTime.now());
        applyBillingCardMeta(a, issue.cardMeta());
        academyRepo.save(a);
        log.info("[Billing] changeBillingCard academyId={}", academyId);
        return getSummary(academyId);
    }

    /** 포인트 충전(결제 완료 후 호출). */
    public BillingSummaryResponse chargePoints(Long academyId, BillingPointChargeRequest req) {
        int pts = req.getPoints();
        if (pts != 5_000 && pts != 10_000 && pts != 20_000 && pts != 30_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "충전 금액은 5,000 / 10,000 / 20,000 / 30,000원(VAT 포함)만 가능합니다.");
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

    /** 매일 새벽 만료된 구독을 자동 청구(중복 방지용 월별 orderId 사용) */
    @Scheduled(cron = "0 15 3 * * *", zone = "Asia/Seoul")
    public void runAutoBillingScheduler() {
        if (tossSecretKey == null || tossSecretKey.isBlank()) {
            log.warn("[Billing] auto charge skipped: toss secret key is missing");
            return;
        }
        List<Academy> academies = academyRepo.findAll();
        LocalDateTime now = LocalDateTime.now();
        for (Academy a : academies) {
            try {
                if (!Boolean.TRUE.equals(a.getAutoBillingEnabled())) continue;
                if (a.getTossBillingKey() == null || a.getTossBillingKey().isBlank() || a.getTossCustomerKey() == null || a.getTossCustomerKey().isBlank()) continue;
                if (a.getSubscriptionEndsAt() == null || now.isBefore(a.getSubscriptionEndsAt())) continue;

                String monthlyOrderId = "AUTO-" + a.getId() + "-" + now.format(DateTimeFormatter.ofPattern("yyyyMM"));
                if (paymentLogRepo.existsByAcademy_IdAndOrderId(a.getId(), monthlyOrderId)) continue;

                long amount = monthlyAmountForPlan(a.getBillingPlanId());
                chargeWithBillingKey(a.getTossBillingKey(), a.getTossCustomerKey(), amount, monthlyOrderId, "하이아카데미 월 자동청구");

                BillingSubscribeRequest req = new BillingSubscribeRequest();
                req.setPlanId(a.getBillingPlanId());
                req.setBillingCycle("MONTHLY");
                req.setOrderId(monthlyOrderId);
                req.setPaidAmountKrw(amount);
                subscribe(a.getId(), req);
                log.info("[Billing] auto charge success academyId={} amount={} orderId={}", a.getId(), amount, monthlyOrderId);
            } catch (Exception e) {
                log.warn("[Billing] auto charge failed academyId={} reason={}", a.getId(), e.getMessage());
                a.setBillingStatus("PAST_DUE");
                academyRepo.save(a);
            }
        }
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
            return "베이직";
        }
        return switch (planId.trim().toLowerCase()) {
            case "basic" -> "베이직";
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
        SmsCosts smsCosts = loadSmsCosts();
        int cost = switch (req.getType().trim().toUpperCase()) {
            case "GENERAL" -> smsCosts.sms();
            case "PAYMENT_NUDGE", "PAYMENT_SMS" -> smsCosts.paymentSms();
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

    public SmsPointDeductionResult deductSmsPoints(Long academyId, String messageType, String recipientPhone, int recipientCount) {
        Academy a = loadAndMigrate(academyId);
        SmsCosts smsCosts = loadSmsCosts();
        int count = Math.max(recipientCount, 1);
        String normalizedType = (messageType == null ? "" : messageType.trim().toUpperCase(Locale.ROOT));
        int perCost = switch (normalizedType) {
            case "PAYMENT_SMS", "PAYMENT_NUDGE" -> smsCosts.paymentSms();
            case "KAKAO_ALIMTALK" -> smsCosts.kakaoAlimtalk();
            case "SMS", "GENERAL" -> smsCosts.sms();
            case "LMS" -> smsCosts.lms();
            case "MMS" -> smsCosts.mms();
            default -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "messageType은 KAKAO_ALIMTALK, PAYMENT_SMS, SMS, LMS, MMS 중 하나여야 합니다."
            );
        };
        int totalCost = perCost * count;
        int pts = a.getSmsPoints() != null ? a.getSmsPoints() : 0;
        if (pts < totalCost) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format(Locale.KOREA, "포인트가 부족합니다. 필요 %,dP / 보유 %,dP", totalCost, pts)
            );
        }
        a.setSmsPoints(pts - totalCost);
        academyRepo.save(a);
        log.info(
            "[Billing] deductSmsPoints academyId={} type={} recipient={} count={} cost={} remaining={}",
            academyId, messageType, recipientPhone, count, totalCost, a.getSmsPoints()
        );
        return new SmsPointDeductionResult(normalizedType, totalCost, a.getSmsPoints());
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

    private SmsCosts loadSmsCosts() {
        Map<String, Integer> costs = readSmsCostsFromCommonCodes();
        Integer kakaoAlimtalk = costs.get("KAKAO_ALIMTALK");
        Integer sms = costs.get("SMS");
        Integer lms = costs.get("LMS");
        Integer mms = costs.get("MMS");
        Integer paymentSms = costs.get("PAYMENT_SMS");
        if (kakaoAlimtalk == null || sms == null || lms == null || mms == null || paymentSms == null) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "문자 단가 설정이 누락되었습니다. common_codes(MESSAGE_POINT_COST)의 KAKAO_ALIMTALK/SMS/LMS/MMS/PAYMENT_SMS를 확인해 주세요."
            );
        }
        return new SmsCosts(kakaoAlimtalk, sms, lms, mms, paymentSms);
    }

    private Map<String, Integer> readSmsCostsFromCommonCodes() {
        // Try common schema variations to avoid hard dependency on one naming convention.
        List<String> queries = List.of(
            "select code, code_value from common_codes where code_group = 'MESSAGE_POINT_COST' and use_yn = 'Y'",
            "select code_key as code, code_value from common_codes where code_group = 'MESSAGE_POINT_COST' and use_yn = 'Y'",
            "select code, code_value from common_codes where group_code = 'MESSAGE_POINT_COST' and use_yn = 'Y'",
            "select code_key as code, code_value from common_codes where group_code = 'MESSAGE_POINT_COST' and use_yn = 'Y'",
            "select code, value as code_value from common_codes where code_group = 'MESSAGE_POINT_COST' and use_yn = 'Y'",
            "select code_key as code, value as code_value from common_codes where code_group = 'MESSAGE_POINT_COST' and use_yn = 'Y'",
            "select code, value as code_value from common_codes where group_code = 'MESSAGE_POINT_COST' and use_yn = 'Y'",
            "select code_key as code, value as code_value from common_codes where group_code = 'MESSAGE_POINT_COST' and use_yn = 'Y'"
        );

        ResponseStatusException lastError = null;
        for (String sql : queries) {
            try {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                if (rows.isEmpty()) {
                    continue;
                }
                Map<String, Integer> parsed = new java.util.HashMap<>();
                for (Map<String, Object> row : rows) {
                    String code = asUpperString(row.get("code"));
                    Integer value = asPositiveInt(row.get("code_value"));
                    if (code == null || value == null) continue;
                    parsed.put(code, value);
                }
                if (!parsed.isEmpty()) return parsed;
            } catch (Exception e) {
                lastError = new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "문자 단가(common_codes) 조회에 실패했습니다.",
                    e
                );
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "문자 단가(common_codes)를 찾을 수 없습니다. MESSAGE_POINT_COST 그룹 데이터를 확인해 주세요."
        );
    }

    private static String asUpperString(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return null;
        return text.toUpperCase(Locale.ROOT);
    }

    private static Integer asPositiveInt(Object value) {
        if (value == null) return null;
        try {
            int parsed = Integer.parseInt(String.valueOf(value).trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record SmsCosts(int kakaoAlimtalk, int sms, int lms, int mms, int paymentSms) {}

    public record SmsPointDeductionResult(String messageType, int deductedPoints, int remainingPoints) {}

    /**
     * 요금제 변경 가능 여부: 등록 학생 수만 검사합니다.
     * 정기결제 등록 여부와 관계없이 업그레이드·다운그레이드 모두 허용하며,
     * 다운그레이드 시에는 변경 후 요금제의 학생 상한만 확인합니다.
     */
    private void validatePlanChange(Academy academy, String newPlanId) {
        String next = BillingPlanLimits.normalizePlanId(newPlanId);
        long studentCount = studentRepo.countByAcademyIdExcludingWithdrawn(academy.getId(), StudentStatus.퇴원);
        int newMax = BillingPlanLimits.maxStudents(next);
        if (studentCount > newMax) {
            String targetPlanName = planLabel(next);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                String.format(
                    "현재 등록 학생 %d명입니다. %s 요금제는 최대 %d명까지 가능합니다. "
                        + "학부모 관리에서 등록 인원을 삭제한 후 요금제를 변경해 주세요.",
                    studentCount, targetPlanName, newMax));
        }
    }

    /** @deprecated 호환용 — {@link #changeBillingPlan} 과 동일 */
    public BillingSummaryResponse upgradePlan(Long academyId, BillingSubscribeRequest req) {
        return changeBillingPlan(academyId, req);
    }

    private long monthlyAmountForPlan(String planId) {
        String normalized = BillingPlanLimits.normalizePlanId(planId);
        return switch (normalized) {
            case "basic" -> 4_400L;
            case "standard" -> 8_800L;
            case "premium" -> 16_500L;
            case "enterprise" -> 33_000L;
            default -> 4_400L;
        };
    }

    private TossBillingKeyIssueResult issueBillingKey(BillingAutoSubscribeRequest req) {
        ensureTossSecretConfigured();
        HttpHeaders headers = tossHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("customerKey", req.getCustomerKey());
            payload.put("cardNumber", req.getCardNumber().replaceAll("\\s+", ""));
            payload.put("cardExpirationYear", req.getCardExpirationYear());
            payload.put("cardExpirationMonth", req.getCardExpirationMonth());
            payload.put("customerIdentityNumber", req.getCustomerIdentityNumber());
            payload.put("cardPassword", req.getCardPassword());
            if (req.getCustomerName() != null && !req.getCustomerName().isBlank()) {
                payload.put("customerName", req.getCustomerName());
            }
            if (req.getCustomerEmail() != null && !req.getCustomerEmail().isBlank()) {
                payload.put("customerEmail", req.getCustomerEmail());
            }

            ResponseEntity<byte[]> res = restTemplate.postForEntity(
                "https://api.tosspayments.com/v1/billing/authorizations/card",
                new HttpEntity<>(objectMapper.writeValueAsString(payload), headers),
                byte[].class
            );
            String responseBody = decodeUtf8Response(res.getBody());
            JsonNode root = objectMapper.readTree(responseBody.isBlank() ? "{}" : responseBody);
            String billingKey = root.path("billingKey").asText("");
            if (billingKey.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "토스 빌링키 발급에 실패했습니다.");
            }
            BillingCardMeta cardMeta = parseBillingCardMeta(root, req);
            return new TossBillingKeyIssueResult(billingKey, cardMeta);
        } catch (HttpStatusCodeException e) {
            throw tossException("빌링키 발급 실패", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "토스 빌링키 발급 중 오류가 발생했습니다.", e);
        }
    }

    private void chargeWithBillingKey(String billingKey, String customerKey, long amount, String orderId, String orderName) {
        ensureTossSecretConfigured();
        HttpHeaders headers = tossHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String payload = String.format(
            "{\"customerKey\":\"%s\",\"amount\":%d,\"orderId\":\"%s\",\"orderName\":\"%s\"}",
            customerKey, amount, orderId, orderName
        );
        try {
            restTemplate.postForEntity(
                "https://api.tosspayments.com/v1/billing/" + billingKey,
                new HttpEntity<>(payload, headers),
                String.class
            );
        } catch (HttpStatusCodeException e) {
            throw tossException("자동청구 실패", e);
        }
    }

    private HttpHeaders tossHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String raw = tossSecretKey + ":";
        String encoded = Base64.getEncoder().encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encoded);
        return headers;
    }

    private void ensureTossSecretConfigured() {
        if (tossSecretKey == null || tossSecretKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "토스 시크릿 키가 설정되지 않았습니다.");
        }
    }

    private ResponseStatusException tossException(String prefix, HttpStatusCodeException e) {
        String body = decodeUtf8Response(e.getResponseBodyAsByteArray());
        String message = prefix;
        try {
            JsonNode root = objectMapper.readTree(body.isBlank() ? "{}" : body);
            String tossMsg = root.path("message").asText("");
            if (!tossMsg.isBlank()) {
                message = prefix + ": " + tossMsg;
            }
        } catch (Exception ignore) {
            // ignore parse failure
        }
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message, e);
    }

    private record BillingCardMeta(String last4, String cardCompany, String issuerCode, String expMonth, String expYear) {}

    private record TossBillingKeyIssueResult(String billingKey, BillingCardMeta cardMeta) {}

    private void applyBillingCardMeta(Academy academy, BillingCardMeta meta) {
        if (meta == null) return;
        if (meta.last4() != null && !meta.last4().isBlank()) {
            academy.setBillingCardLast4(meta.last4());
        }
        if (meta.issuerCode() != null && !meta.issuerCode().isBlank()) {
            academy.setBillingCardIssuerCode(meta.issuerCode());
        }
        if (meta.cardCompany() != null && !meta.cardCompany().isBlank()) {
            academy.setBillingCardCompany(meta.cardCompany());
        }
        if (meta.expMonth() != null && !meta.expMonth().isBlank()) {
            academy.setBillingCardExpMonth(meta.expMonth());
        }
        if (meta.expYear() != null && !meta.expYear().isBlank()) {
            academy.setBillingCardExpYear(meta.expYear());
        }
    }

    private BillingCardMeta parseBillingCardMeta(JsonNode root, BillingAutoSubscribeRequest req) {
        JsonNode card = root.path("card");
        String issuerCode = card.path("issuerCode").asText("").trim();
        if (issuerCode.isBlank()) {
            issuerCode = card.path("acquirerCode").asText("").trim();
        }
        String cardCompanyRaw = root.path("cardCompany").asText("").trim();
        String cardCompany = TossCardCompanyNames.resolveDisplayName(issuerCode, cardCompanyRaw);
        String maskedNumber = root.path("cardNumber").asText("").trim();
        if (maskedNumber.isBlank()) {
            maskedNumber = card.path("number").asText("").trim();
        }
        String last4 = extractLast4Digits(maskedNumber);
        if (last4.isBlank() && req != null && req.getCardNumber() != null) {
            last4 = extractLast4Digits(req.getCardNumber().replaceAll("\\s+", ""));
        }
        String expMonth = req != null ? normalizeExpMonth(req.getCardExpirationMonth()) : "";
        String expYear = req != null ? normalizeExpYear(req.getCardExpirationYear()) : "";
        return new BillingCardMeta(last4, cardCompany, issuerCode, expMonth, expYear);
    }

    private static String resolveStoredCardCompany(Academy academy) {
        return TossCardCompanyNames.resolveDisplayName(
            academy.getBillingCardIssuerCode(),
            academy.getBillingCardCompany());
    }

    private void maybeRepairStoredCardCompany(Academy academy) {
        if (academy.getBillingCardLast4() == null || academy.getBillingCardLast4().isBlank()) {
            return;
        }
        String resolved = resolveStoredCardCompany(academy);
        if (resolved == null || resolved.isBlank()) {
            return;
        }
        if (!resolved.equals(academy.getBillingCardCompany())) {
            academy.setBillingCardCompany(resolved);
            academyRepo.save(academy);
        }
    }

    private static String decodeUtf8Response(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }
        return new String(body, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String extractLast4Digits(String cardNumberField) {
        if (cardNumberField == null || cardNumberField.isBlank()) {
            return "";
        }
        String digits = cardNumberField.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return digits;
        }
        return digits.substring(digits.length() - 4);
    }

    private static String normalizeExpMonth(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String m = raw.replaceAll("\\D", "");
        if (m.isEmpty()) return "";
        if (m.length() >= 2) {
            return m.substring(m.length() - 2);
        }
        try {
            return String.format(Locale.KOREA, "%02d", Integer.parseInt(m));
        } catch (NumberFormatException e) {
            return m;
        }
    }

    private static String normalizeExpYear(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String y = raw.replaceAll("\\D", "");
        if (y.length() >= 4) {
            return y.substring(y.length() - 4);
        }
        if (y.length() == 2) {
            return y;
        }
        return y;
    }

}
