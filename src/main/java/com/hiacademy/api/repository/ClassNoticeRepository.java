package com.hiacademy.api.repository;

import com.hiacademy.api.entity.ClassNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassNoticeRepository extends JpaRepository<ClassNotice, Long> {
    List<ClassNotice> findAllByAcademy_IdOrderByCreatedAtDesc(Long academyId);
    Optional<ClassNotice> findByIdAndAcademy_Id(Long id, Long academyId);

    @Query("""
        select distinct n
        from ClassNotice n
        left join n.targets t
        where n.academy.id = :academyId
          and (:target is null or t = :target or t = '전체')
          and (
               :restrictTargets = false
               or t = '전체'
               or t in :allowedTargets
          )
          and (:q is null or :q = '' or
               lower(n.title) like lower(concat('%', :q, '%')) or
               lower(n.body)  like lower(concat('%', :q, '%')) or
               lower(n.date)  like lower(concat('%', :q, '%')) or
               lower(t)        like lower(concat('%', :q, '%'))
          )
        """)
    Page<ClassNotice> search(
        @Param("academyId") Long academyId,
        @Param("target") String target,
        @Param("q") String q,
        @Param("restrictTargets") boolean restrictTargets,
        @Param("allowedTargets") List<String> allowedTargets,
        Pageable pageable
    );
}
