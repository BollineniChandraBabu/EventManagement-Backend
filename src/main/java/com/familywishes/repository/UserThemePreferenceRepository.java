package com.familywishes.repository;

import com.familywishes.entity.UserThemePreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserThemePreferenceRepository extends JpaRepository<UserThemePreference, Long> {
  Optional<UserThemePreference> findByUserId(Long userId);
}
