package com.familywishes.repository;

import com.familywishes.entity.EventTypeSeed;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventTypeSeedRepository extends JpaRepository<EventTypeSeed, Long> {
  Optional<EventTypeSeed> findByCode(String code);

  Optional<EventTypeSeed> findByCodeAndActiveTrue(String code);

  List<EventTypeSeed> findByActiveTrueOrderByDisplayNameAsc();

  @Query(
      """
      SELECT e FROM EventTypeSeed e
      WHERE (:searchKey = ''
             OR LOWER(e.code) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(e.displayName) LIKE LOWER(CONCAT('%', :searchKey, '%')))
      """)
  Page<EventTypeSeed> findAllBySearchKey(@Param("searchKey") String searchKey, Pageable pageable);
}
