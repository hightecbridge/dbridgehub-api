package com.hiacademy.api.repository;

import com.hiacademy.api.entity.BillingPaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingPaymentLogRepository extends JpaRepository<BillingPaymentLog, Long> {
    List<BillingPaymentLog> findByAcademy_IdOrderByCreatedAtDesc(Long academyId);
}
