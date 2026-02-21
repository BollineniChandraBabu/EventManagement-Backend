package com.familywishes.repository;

import com.familywishes.entity.WishSeedTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishSeedTemplateRepository extends JpaRepository<WishSeedTemplate, Long> {
  Optional<WishSeedTemplate> findByType_Code(String type);

  Optional<WishSeedTemplate> findByType_CodeAndActiveTrue(String type);
}
