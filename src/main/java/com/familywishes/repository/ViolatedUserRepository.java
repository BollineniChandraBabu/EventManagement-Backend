package com.familywishes.repository;

import com.familywishes.entity.ViolatedUser;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ViolatedUserRepository extends JpaRepository<ViolatedUser, Long> {
  List<ViolatedUser> findByEmail(String email);

  Page<ViolatedUser> findByLoggedInAtGreaterThanEqualAndLoggedInAtLessThanEqual(
      LocalDateTime start, LocalDateTime end, Pageable pageable);

  List<ViolatedUser> findByLoggedInAtGreaterThanEqualAndLoggedInAtLessThanEqual(
      LocalDateTime start, LocalDateTime end);

  long countByLoggedInAtGreaterThanEqualAndLoggedInAtLessThanEqual(
      LocalDateTime start, LocalDateTime end);

  @Query(
      """
          SELECT DATE(v.loggedInAt), COUNT(v)
          FROM ViolatedUser v
          WHERE v.loggedInAt >= :start
            AND v.loggedInAt <= :end
          GROUP BY DATE(v.loggedInAt)
          ORDER BY DATE(v.loggedInAt)
          """)
  List<Object[]> getDailyCountsByLoggedInAtBetween(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
