package com.hiacademy.api.repository;

import com.hiacademy.api.entity.MessageSendLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageSendLogRepository extends JpaRepository<MessageSendLog, Long> {
    List<MessageSendLog> findTop50ByAcademy_IdOrderByCreatedAtDesc(Long academyId);
}
