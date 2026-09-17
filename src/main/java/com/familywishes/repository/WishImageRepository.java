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
      SELECT w FROM WishImage w
      WHERE (:searchKey = '' OR LOWER(w.eventType) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(COALESCE(w.user.name, '')) LIKE LOWER(CONCAT('%', :searchKey, '%')))
        AND (:eventType = '' OR LOWER(w.eventType) = LOWER(:eventType))
        AND (:userId IS NULL OR w.user.id = :userId)
        AND (:active IS NULL OR w.active = :active)
      """)
  Page<WishImage> findAllFiltered(
      @Param("searchKey") String searchKey,
      @Param("eventType") String eventType,
      @Param("userId") Long userId,
      @Param("active") Boolean active,
      Pageable pageable);
}
