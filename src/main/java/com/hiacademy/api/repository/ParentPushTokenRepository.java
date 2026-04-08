package com.hiacademy.api.repository;

import com.hiacademy.api.entity.ParentPushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParentPushTokenRepository extends JpaRepository<ParentPushToken, Long> {
    Optional<ParentPushToken> findByExpoPushToken(String expoPushToken);

    List<ParentPushToken> findAllByParent_IdIn(Collection<Long> parentIds);

    void deleteAllByParent_Id(Long parentId);
}
