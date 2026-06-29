package com.familywishes.repository;

import com.familywishes.entity.FestivalWishMapping;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FestivalWishMappingRepository extends JpaRepository<FestivalWishMapping, Long> {

  List<FestivalWishMapping> findBySpecialEvent_EventDateAndActiveTrue(LocalDate eventDate);

  Optional<FestivalWishMapping> findBySpecialEvent_IdAndUser_Id(Long specialEventId, Long userId);

  @Query(
      """
      SELECT f FROM FestivalWishMapping f
      WHERE (:searchKey = ''
             OR LOWER(f.specialEvent.eventName) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(f.user.name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(COALESCE(f.customMessage, '')) LIKE LOWER(CONCAT('%', :searchKey, '%')))
      """)
  Page<FestivalWishMapping> findAllBySearchKey(
      @Param("searchKey") String searchKey, Pageable pageable);

  @Query(
      """
      SELECT f FROM FestivalWishMapping f
      WHERE f.user.id = :userId
        AND f.active = true
        AND (:searchKey = ''
             OR LOWER(f.specialEvent.eventName) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(COALESCE(f.customMessage, '')) LIKE LOWER(CONCAT('%', :searchKey, '%')))
      """)
  Page<FestivalWishMapping> findActiveByUserIdAndSearchKey(
      @Param("userId") Long userId, @Param("searchKey") String searchKey, Pageable pageable);
}
