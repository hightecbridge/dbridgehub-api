package com.hiacademy.api.repository;

import com.hiacademy.api.entity.NoticeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<NoticeItem, Long> {
    List<NoticeItem> findAllByAcademy_IdOrderByCreatedAtDesc(Long academyId);
    List<NoticeItem> findAllByOrderByCreatedAtDesc();
    Page<NoticeItem> findAllByAcademy_IdOrderByCreatedAtDesc(Long academyId, Pageable pageable);
    Page<NoticeItem> findAllByOrderByCreatedAtDesc(Pageable pageable);
    @Query("""
        select distinct n
        from NoticeItem n
        left join n.targets t
        where n.academy.id = :academyId
          and (:target is null or t = :target or t = '전체')
          and (:q is null or :q = '' or
               lower(n.title) like lower(concat('%', :q, '%')) or
               lower(n.body)  like lower(concat('%', :q, '%')) or
               lower(n.date)  like lower(concat('%', :q, '%')) or
               lower(t)        like lower(concat('%', :q, '%'))
          )
        """)
    Page<NoticeItem> search(
        @Param("academyId") Long academyId,
        @Param("target") String target,
        @Param("q") String q,
        Pageable pageable
    );
    Optional<NoticeItem> findByIdAndAcademy_Id(Long id, Long academyId);
}
