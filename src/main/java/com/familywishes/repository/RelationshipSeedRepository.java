package com.familywishes.repository;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;
import com.familywishes.entity.RelationshipSeed;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RelationshipSeedRepository extends JpaRepository<RelationshipSeed, Long> {
  Optional<RelationshipSeed> findByCode(String code);

  @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
  Optional<RelationshipSeed> findByCodeAndActiveTrue(String code);

  List<RelationshipSeed> findByActiveTrueOrderByDisplayNameAsc();

  @Query(
      """
      SELECT r FROM RelationshipSeed r
      WHERE (:searchKey = ''
             OR LOWER(r.code) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(r.displayName) LIKE LOWER(CONCAT('%', :searchKey, '%')))
        AND (:codes IS NULL OR r.code IN :codes)
        AND (:activeFilter = -1
         OR (:activeFilter = 1 AND active = true)
         OR (:activeFilter = 0 AND active = false))
      """)
  Page<RelationshipSeed> findAllByFilters(
      @Param("searchKey") String searchKey,
      @Param("codes") List<String> codes,
      @Param("activeFilter") Integer activeFilter,
      Pageable pageable);

  @Query(
      """
      SELECT r FROM RelationshipSeed r
      WHERE (:searchKey = ''
             OR LOWER(r.code) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(r.displayName) LIKE LOWER(CONCAT('%', :searchKey, '%')))
      """)
  Page<RelationshipSeed> findAllBySearchKey(
      @Param("searchKey") String searchKey, Pageable pageable);
}
