package com.familywishes.repository;

import com.familywishes.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

  Optional<RefreshToken> findByToken(String token);

  @Transactional
  long deleteByExpiresAtBefore(LocalDateTime expiresAt);

  @Modifying
  @Transactional
  @Query(
      "UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId AND r.revoked = false")
  int revokeAllByUserId(@Param("userId") Long userId);
}
