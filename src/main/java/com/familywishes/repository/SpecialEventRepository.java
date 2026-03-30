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

  @Query(value = """
    SELECT * FROM seed_special_events s
    WHERE (:month IS NULL OR EXTRACT(MONTH FROM s.event_date) = :month)
    ORDER BY s.event_date, s.event_name
""", nativeQuery = true)
  List<SpecialEvent> findByMonth(@Param("month") Integer month);

  @Query(
      """
      SELECT s FROM SpecialEvent s
      WHERE (:month IS NULL OR FUNCTION('MONTH', s.eventDate) = :month)
        AND (:searchKey = ''
             OR LOWER(s.eventName) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(COALESCE(s.message, '')) LIKE LOWER(CONCAT('%', :searchKey, '%')))
      """)
  Page<SpecialEvent> findByMonthAndSearchKey(
      @Param("month") Integer month, @Param("searchKey") String searchKey, Pageable pageable);

  Optional<SpecialEvent> findByEventNameIgnoreCase(String eventName);

  @Query(
      """
            SELECT s FROM SpecialEvent s
            WHERE :searchKey = ''
            OR LOWER(s.eventName) LIKE LOWER(CONCAT('%', :searchKey, '%'))
            OR LOWER(s.message) LIKE LOWER(CONCAT('%', :searchKey, '%'))
            """)
  Page<SpecialEvent> findAllBySearchKey(@Param("searchKey") String searchKey, Pageable pageable);
}
