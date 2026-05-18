package com.familywishes.repository;

import com.familywishes.entity.LoginLocationEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginLocationEventRepository extends JpaRepository<LoginLocationEvent, Long> {
  @Query(
      """
      SELECT e.loginLocation, COUNT(e)
      FROM LoginLocationEvent e
      WHERE (:userId IS NULL OR e.user.id = :userId)
        AND (:fromDate IS NULL OR e.loggedInAt >= :fromDate)
        AND (:toDate IS NULL OR e.loggedInAt <= :toDate)
      GROUP BY e.loginLocation
      ORDER BY COUNT(e) DESC
      """)
  List<Object[]> countByLocationWithFilters(
      @Param("userId") Long userId,
      @Param("fromDate") LocalDateTime fromDate,
      @Param("toDate") LocalDateTime toDate);
}
