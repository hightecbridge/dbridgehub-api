package com.hiacademy.api.repository;

import com.hiacademy.api.entity.SenderNumberItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SenderNumberRepository extends JpaRepository<SenderNumberItem, Long> {
    List<SenderNumberItem> findAllByAcademy_IdOrderByCreatedAtDesc(Long academyId);
    Optional<SenderNumberItem> findByIdAndAcademy_Id(Long id, Long academyId);
    void deleteByIdAndAcademy_Id(Long id, Long academyId);
}

