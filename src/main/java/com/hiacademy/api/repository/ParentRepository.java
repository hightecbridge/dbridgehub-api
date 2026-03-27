package com.hiacademy.api.repository;

import com.hiacademy.api.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParentRepository extends JpaRepository<Parent, Long> {
    List<Parent> findAllByAcademy_IdOrderByCreatedAtDesc(Long academyId);
    Optional<Parent> findByIdAndAcademy_Id(Long id, Long academyId);
    Optional<Parent> findByLoginPhone(String loginPhone);
    boolean existsByLoginPhone(String loginPhone);
}
