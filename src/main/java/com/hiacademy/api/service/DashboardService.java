package com.hiacademy.api.service;

import com.hiacademy.api.dto.response.DashboardStatsResponse;
import com.hiacademy.api.entity.FeeRecord;
import com.hiacademy.api.entity.Student;
import com.hiacademy.api.repository.FeeRecordRepository;
import com.hiacademy.api.repository.StudentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {
    private final StudentRepository studentRepo;
    private final FeeRecordRepository feeRepo;
    private final AdminAccessService access;

    public DashboardService(StudentRepository studentRepo, FeeRecordRepository feeRepo, AdminAccessService access) {
        this.studentRepo = studentRepo;
        this.feeRepo = feeRepo;
        this.access = access;
    }

    public DashboardStatsResponse stats(Authentication auth, Integer year) {
        AdminAccessService.Scope scope = access.resolve(auth);
        int y = year != null && year >= 2000 && year <= 2100 ? year : LocalDate.now().getYear();
        int[] enrolled = new int[13];
        int[] withdrawn = new int[13];
        long[] revenue = new long[13];
        long[] unpaid = new long[13];

        for (Student s : studentRepo.findAllByAcademyIdWithClassroom(scope.academyId())) {
            if (!scope.allowsStudent(s)) continue;
            LocalDateTime created = s.getCreatedAt();
            if (created != null && created.getYear() == y) {
                enrolled[created.getMonthValue()]++;
            }
            LocalDateTime left = s.getWithdrawnAt();
            if (left != null && left.getYear() == y) {
                withdrawn[left.getMonthValue()]++;
            }
        }

        int fromYm = y * 100 + 1;
        int toYm = y * 100 + 12;
        for (FeeRecord f : feeRepo.findAllByAcademyIdAndYearMonthBetween(scope.academyId(), fromYm, toYm)) {
            if (f.getStudent() == null || !scope.allowsStudent(f.getStudent())) continue;
            int m = f.getYearMonth() % 100;
            if (m < 1 || m > 12) continue;
            if (f.isPaid()) revenue[m] += f.getAmount();
            else unpaid[m] += f.getAmount();
        }

        List<DashboardStatsResponse.MonthItem> months = new ArrayList<>();
        int yearEnrolled = 0;
        int yearWithdrawn = 0;
        long yearRevenue = 0;
        long yearUnpaid = 0;
        for (int m = 1; m <= 12; m++) {
            months.add(DashboardStatsResponse.MonthItem.builder()
                .month(m)
                .enrolled(enrolled[m])
                .withdrawn(withdrawn[m])
                .revenue(revenue[m])
                .unpaid(unpaid[m])
                .build());
            yearEnrolled += enrolled[m];
            yearWithdrawn += withdrawn[m];
            yearRevenue += revenue[m];
            yearUnpaid += unpaid[m];
        }
        return DashboardStatsResponse.builder()
            .year(y)
            .yearEnrolled(yearEnrolled)
            .yearWithdrawn(yearWithdrawn)
            .yearRevenue(yearRevenue)
            .yearUnpaid(yearUnpaid)
            .months(months)
            .build();
    }
}
