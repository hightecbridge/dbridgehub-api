package com.hiacademy.api.repository;

import com.hiacademy.api.entity.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {
    List<ClassRoom> findAllByAcademy_IdOrderByCreatedAtAsc(Long academyId);
    Optional<ClassRoom> findByIdAndAcademy_Id(Long id, Long academyId);
}
