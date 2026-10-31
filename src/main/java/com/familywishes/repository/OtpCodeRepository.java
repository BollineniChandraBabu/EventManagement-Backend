package com.familywishes.repository;

import com.familywishes.entity.OtpCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {
  Optional<OtpCode> findTopByUserIdOrderByIdDesc(Long userId);

  Optional<OtpCode> findTopByUserIdAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
      Long userId, java.time.LocalDateTime now);
}
