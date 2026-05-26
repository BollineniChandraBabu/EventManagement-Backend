package com.familywishes.repository;

import com.familywishes.entity.LoginLocationEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginLocationEventRepository extends JpaRepository<LoginLocationEvent, Long> {

  @Query("""
          SELECT e
          FROM LoginLocationEvent e
          WHERE (:userIds IS NULL OR e.user.id IN (:userIds))
            AND e.loggedInAt >= COALESCE(:fromDate, e.loggedInAt)
            AND e.loggedInAt <= COALESCE(:toDate, e.loggedInAt)
          ORDER BY e.loggedInAt DESC
          """)
  List<LoginLocationEvent> countByLocationWithFilters(
          @Param("userIds") List<Long> userIds,
          @Param("fromDate") LocalDateTime fromDate,
          @Param("toDate") LocalDateTime toDate
  );
}
