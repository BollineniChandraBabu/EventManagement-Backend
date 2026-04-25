package com.familywishes.repository;

import com.familywishes.entity.Event;
import java.time.LocalDate;
import java.util.List;

import com.familywishes.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

  @Query("SELECT e FROM Event e WHERE MONTH(e.eventDate) = :month AND DAY(e.eventDate) = :day")
  List<Event> findTodaysEvents(int month, int day);

  long countByEventDateGreaterThanEqualAndActiveTrue(LocalDate eventDate);

  long countByUser_IdAndEventDateGreaterThanEqualAndActiveTrue(Long userId, LocalDate eventDate);

  List<Event> findByEventType_CodeInAndActiveTrue(List<String> eventTypes);

  @Query(
      """
            SELECT e FROM Event e
            WHERE :searchKey = ''
               OR LOWER(CAST(e.eventType.code as string)) LIKE LOWER(CONCAT('%', :searchKey, '%'))
            """)
  Page<Event> findAllBySearchKey(@Param("searchKey") String searchKey, Pageable pageable);

  @Query(
          """
                SELECT e FROM Event e
                WHERE e.user.email = :userEmail
                AND ( :searchKey = ''
                   OR LOWER(CAST(e.eventType.code as string)) LIKE LOWER(CONCAT('%', :searchKey, '%')))
                """)
  Page<Event> findAllBySearchKeyAndUSer( @Param("userEmail") String userEmail, @Param("searchKey") String searchKey, Pageable pageable);
}
