package com.hiacademy.api.repository;

import com.hiacademy.api.entity.MessageSenderProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageSenderProfileRepository extends JpaRepository<MessageSenderProfile, Long> {
    Optional<MessageSenderProfile> findFirstByAdminUserIdAndActiveTrue(Long adminUserId);

    Optional<MessageSenderProfile> findFirstByAdminUserIdIsNullAndActiveTrueOrderByIdAsc();
}
