package com.familywishes.repository;

import com.familywishes.entity.SpecialEvent;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpecialEventRepository extends JpaRepository<SpecialEvent, Long> {

  List<SpecialEvent> findByEventDateAndActiveTrue(LocalDate eventDate);

  @Query(
      """
            SELECT s FROM SpecialEvent s
            WHERE (:month IS NULL OR FUNCTION('month', s.eventDate) = :month)
            ORDER BY s.eventDate ASC, s.eventName ASC
            """)
  List<SpecialEvent> findByMonth(@Param("month") Integer month);

  Optional<SpecialEvent> findByExternalEventId(String externalEventId);

  @Query(
      """
            SELECT s FROM SpecialEvent s
            WHERE :searchKey = ''
               OR LOWER(s.eventName) LIKE LOWER(CONCAT('%', :searchKey, '%'))
               OR LOWER(s.message) LIKE LOWER(CONCAT('%', :searchKey, '%'))
            """)
  Page<SpecialEvent> findAllBySearchKey(@Param("searchKey") String searchKey, Pageable pageable);
}
