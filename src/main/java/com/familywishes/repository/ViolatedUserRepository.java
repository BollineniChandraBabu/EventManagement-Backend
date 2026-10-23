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

  /** Attempts in the range whose email or IP address contains the search text. */
  @Query(
      """
          SELECT v
          FROM ViolatedUser v
          WHERE v.loggedInAt >= :start
            AND v.loggedInAt <= :end
            AND (:searchKey = ''
                 OR LOWER(v.email) LIKE LOWER(CONCAT('%', :searchKey, '%'))
                 OR LOWER(COALESCE(v.ipAddress, '')) LIKE LOWER(CONCAT('%', :searchKey, '%')))
          """)
  Page<ViolatedUser> search(
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end,
      @Param("searchKey") String searchKey,
      Pageable pageable);

  /** Newest attempts that have coordinates (for the map), limited by the pageable. */
  List<ViolatedUser>
      findByLoggedInAtGreaterThanEqualAndLoggedInAtLessThanEqualAndLatitudeIsNotNullAndLongitudeIsNotNull(
          LocalDateTime start, LocalDateTime end, Pageable pageable);

  @Query(
      """
          SELECT COUNT(DISTINCT v.ipAddress)
          FROM ViolatedUser v
          WHERE v.loggedInAt >= :start AND v.loggedInAt <= :end AND v.ipAddress IS NOT NULL
          """)
  long countDistinctIps(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  @Query(
      """
          SELECT COUNT(DISTINCT v.email)
          FROM ViolatedUser v
          WHERE v.loggedInAt >= :start AND v.loggedInAt <= :end AND v.email IS NOT NULL
          """)
  long countDistinctEmails(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  /** Rows of: ip address, attempts, distinct emails, last attempt. Busiest first. */
  @Query(
      """
          SELECT v.ipAddress, COUNT(v), COUNT(DISTINCT v.email), MAX(v.loggedInAt)
          FROM ViolatedUser v
          WHERE v.loggedInAt >= :start AND v.loggedInAt <= :end
            AND v.ipAddress IS NOT NULL AND v.ipAddress <> ''
          GROUP BY v.ipAddress
          ORDER BY COUNT(v) DESC, MAX(v.loggedInAt) DESC
          """)
  List<Object[]> findTopIps(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

  /** Rows of: email, attempts, distinct IPs, last attempt. Busiest first. */
  @Query(
      """
          SELECT v.email, COUNT(v), COUNT(DISTINCT v.ipAddress), MAX(v.loggedInAt)
          FROM ViolatedUser v
          WHERE v.loggedInAt >= :start AND v.loggedInAt <= :end
            AND v.email IS NOT NULL AND v.email <> ''
          GROUP BY v.email
          ORDER BY COUNT(v) DESC, MAX(v.loggedInAt) DESC
          """)
  List<Object[]> findTopEmails(
      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

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
