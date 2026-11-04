package com.familywishes.repository;

import com.familywishes.entity.ThemeSeed;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

public interface ThemeSeedRepository extends JpaRepository<ThemeSeed, Long> {
  Optional<ThemeSeed> findByCode(String code);

  @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
  Optional<ThemeSeed> findByCodeAndActiveTrue(String code);

  List<ThemeSeed> findByActiveTrueOrderByDisplayNameAsc();
}
