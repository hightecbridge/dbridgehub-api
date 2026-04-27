package com.sms.nhn.repository;

import com.sms.nhn.entity.SmsSendHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsSendHistoryRepository extends JpaRepository<SmsSendHistory, Long> {
}
