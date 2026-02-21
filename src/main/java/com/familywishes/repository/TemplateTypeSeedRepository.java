package com.familywishes.repository;

import com.familywishes.entity.TemplateTypeSeed;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateTypeSeedRepository extends JpaRepository<TemplateTypeSeed, Long> {
  Optional<TemplateTypeSeed> findByCode(String code);

  Optional<TemplateTypeSeed> findByCodeAndActiveTrue(String code);

  List<TemplateTypeSeed> findByActiveTrueOrderByDisplayNameAsc();
}
