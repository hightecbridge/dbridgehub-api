package com.hiacademy.api.repository;

import com.hiacademy.api.entity.User;
import com.hiacademy.api.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findAllByAcademy_IdAndRoleOrderByCreatedAtAsc(Long academyId, UserRole role);
    List<User> findAllByAcademy_IdOrderByCreatedAtAsc(Long academyId);
    Optional<User> findFirstByAcademy_IdAndRoleOrderByIdAsc(Long academyId, UserRole role);
    Optional<User> findByIdAndAcademy_Id(Long id, Long academyId);
}
