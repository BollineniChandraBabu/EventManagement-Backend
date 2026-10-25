package com.familywishes.repository;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;
import com.familywishes.entity.TemplateTypeSeed;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateTypeSeedRepository extends JpaRepository<TemplateTypeSeed, Long> {
  Optional<TemplateTypeSeed> findByCode(String code);

  @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
  Optional<TemplateTypeSeed> findByCodeAndActiveTrue(String code);

  List<TemplateTypeSeed> findByActiveTrueOrderByDisplayNameAsc();

  @Query(
      """
      SELECT t FROM TemplateTypeSeed t
      WHERE (:searchKey = ''
             OR LOWER(t.code) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(t.displayName) LIKE LOWER(CONCAT('%', :searchKey, '%')))
        AND (:codes IS NULL OR t.code IN :codes)
        AND (:active IS NULL OR t.active = :active)
      """)
  Page<TemplateTypeSeed> findAllByFilters(
      @Param("searchKey") String searchKey,
      @Param("codes") List<String> codes,
      @Param("active") Boolean active,
      Pageable pageable);

  @Query(
      """
      SELECT t FROM TemplateTypeSeed t
      WHERE (:searchKey = ''
             OR LOWER(t.code) LIKE LOWER(CONCAT('%', :searchKey, '%'))
             OR LOWER(t.displayName) LIKE LOWER(CONCAT('%', :searchKey, '%')))
      """)
  Page<TemplateTypeSeed> findAllBySearchKey(
      @Param("searchKey") String searchKey, Pageable pageable);
}
