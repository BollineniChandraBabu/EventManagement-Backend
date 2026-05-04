package com.familywishes.entity;

import com.familywishes.entity.converter.BooleanToZeroOneConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpCode extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @Column(nullable = false, columnDefinition = "SMALLINT")
  @Convert(converter = BooleanToZeroOneConverter.class)
  private boolean used;
}
