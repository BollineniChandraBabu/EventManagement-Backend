package com.familywishes.repository;

import com.familywishes.entity.Event;
import com.familywishes.entity.enums.EventType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {
  List<Event> findByEventDateAndActiveTrue(LocalDate eventDate);

  long countByEventDateGreaterThanEqualAndActiveTrue(LocalDate eventDate);

  List<Event> findByEventTypeInAndActiveTrue(List<EventType> eventTypes);

  @Query(
      """
            SELECT e FROM Event e
            WHERE :searchKey = ''
               OR LOWER(e.subject) LIKE LOWER(CONCAT('%', :searchKey, '%'))
               OR LOWER(e.body) LIKE LOWER(CONCAT('%', :searchKey, '%'))
               OR LOWER(COALESCE(e.festivalName, '')) LIKE LOWER(CONCAT('%', :searchKey, '%'))
               OR LOWER(CAST(e.eventType as string)) LIKE LOWER(CONCAT('%', :searchKey, '%'))
            """)
  Page<Event> findAllBySearchKey(@Param("searchKey") String searchKey, Pageable pageable);
}
