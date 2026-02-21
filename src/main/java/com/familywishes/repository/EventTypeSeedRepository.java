package com.familywishes.repository;

import com.familywishes.entity.EventTypeSeed;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventTypeSeedRepository extends JpaRepository<EventTypeSeed, Long> {
  Optional<EventTypeSeed> findByCode(String code);

  Optional<EventTypeSeed> findByCodeAndActiveTrue(String code);

  List<EventTypeSeed> findByActiveTrueOrderByDisplayNameAsc();
}
