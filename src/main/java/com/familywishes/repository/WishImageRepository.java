package com.familywishes.repository;

import com.familywishes.entity.WishImage;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WishImageRepository extends JpaRepository<WishImage, Long> {
  List<WishImage> findByEventTypeIgnoreCaseAndActiveTrueAndUser_Id(String eventType, Long userId);

  List<WishImage> findByEventTypeIgnoreCaseAndActiveTrueAndUserIsNull(String eventType);

  @Query(
      """
      SELECT w FROM WishImage w LEFT JOIN w.user u
      WHERE (:searchKey IS NULL OR :searchKey = ''  OR LOWER(w.eventType) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(u.name) LIKE LOWER(CONCAT('%', :searchKey, '%')))
        AND (:eventType IS NULL or :eventType = '' OR LOWER(w.eventType) = LOWER(:eventType))
        AND (:userId IS NULL OR u.id = :userId)
        AND (:activeFilter = -1
        OR (:activeFilter = 1 AND w.active = true)
        OR (:activeFilter = 0 AND w.active = false))
      """)
  Page<WishImage> findAllFiltered(
      @Param("searchKey") String searchKey,
      @Param("eventType") String eventType,
      @Param("userId") Long userId,
      @Param("activeFilter") Integer activeFilter,
      Pageable pageable);
}
