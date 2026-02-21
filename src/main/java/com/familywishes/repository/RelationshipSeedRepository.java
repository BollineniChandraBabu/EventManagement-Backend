package com.familywishes.repository;

import com.familywishes.entity.RelationshipSeed;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelationshipSeedRepository extends JpaRepository<RelationshipSeed, Long> {
  Optional<RelationshipSeed> findByCode(String code);

  Optional<RelationshipSeed> findByCodeAndActiveTrue(String code);

  List<RelationshipSeed> findByActiveTrueOrderByDisplayNameAsc();
}
