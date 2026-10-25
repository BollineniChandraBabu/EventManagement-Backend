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

  List<FestivalWishMapping> findBySpecialEvent_Id(Long specialEventId);

  long countByActiveTrue();

  long countByIsError(boolean isError);

  long countByActiveTrueAndSpecialEvent_EventDateBetween(LocalDate from, LocalDate to);

  @Query(
      """
      SELECT f FROM FestivalWishMapping f
      WHERE (:searchKey = ''
             OR LOWER(f.specialEvent.eventName) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(f.user.name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(f.user.email) LIKE LOWER(CONCAT('%', :searchKey, '%')))
        AND (:specialEventIds IS NULL OR f.specialEvent.id IN :specialEventIds)
        AND (:userIds IS NULL OR f.user.id IN :userIds)
        AND (:statuses IS NULL OR 'ALL' IN :statuses
             OR ('ACTIVE' IN :statuses AND f.active = true)
             OR ('INACTIVE' IN :statuses AND f.active = false)
             OR ('FAILED' IN :statuses AND f.isError = true))
      """)
  Page<FestivalWishMapping> findAllByFilters(
      @Param("searchKey") String searchKey,
      @Param("specialEventIds") List<Long> specialEventIds,
      @Param("userIds") List<Long> userIds,
      @Param("statuses") List<String> statuses,
      Pageable pageable);

  @Query(
      """
      SELECT f FROM FestivalWishMapping f
      WHERE (:searchKey = ''
             OR LOWER(f.specialEvent.eventName) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(f.user.name) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(COALESCE(f.customMessage, '')) LIKE LOWER(CONCAT('%', :searchKey, '%')))
        AND (:status = 'ALL'
             OR (:status = 'ACTIVE' AND f.active = true)
             OR (:status = 'INACTIVE' AND f.active = false)
             OR (:status = 'FAILED' AND f.isError = :errorFlag))
      """)
  Page<FestivalWishMapping> findAllBySearchKey(
      @Param("searchKey") String searchKey,
      @Param("status") String status,
      @Param("errorFlag") boolean errorFlag,
      Pageable pageable);

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
