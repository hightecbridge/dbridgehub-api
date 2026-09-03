package com.hiacademy.api.repository;

import com.hiacademy.api.entity.BillingPointChargeOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BillingPointChargeOrderRepository extends JpaRepository<BillingPointChargeOrder, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from BillingPointChargeOrder o where o.academy.id = :academyId and o.orderId = :orderId")
    Optional<BillingPointChargeOrder> findByAcademy_IdAndOrderId(@Param("academyId") Long academyId, @Param("orderId") String orderId);
}
