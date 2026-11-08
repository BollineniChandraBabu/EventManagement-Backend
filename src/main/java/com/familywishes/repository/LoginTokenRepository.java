package com.familywishes.repository;

import com.familywishes.entity.LoginToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginTokenRepository extends JpaRepository<LoginToken, Long> {
  Optional<LoginToken> findByTokenAndUsedFalse(String token);

  Optional<LoginToken> findTopByUserIdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
      Long userId, LocalDateTime now);
}
