package com.familywishes.repository;

import com.familywishes.entity.WishSeedTemplate;
import com.familywishes.entity.enums.SeedTemplateType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishSeedTemplateRepository extends JpaRepository<WishSeedTemplate, Long> {
  Optional<WishSeedTemplate> findByType(SeedTemplateType type);

  Optional<WishSeedTemplate> findByTypeAndActiveTrue(SeedTemplateType type);
}
