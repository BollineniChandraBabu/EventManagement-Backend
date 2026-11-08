package com.familywishes.entity;

import com.familywishes.entity.converter.BooleanToZeroOneConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * A single-use token that lets a user sign in from an emailed link (for example a circle request)
 * without typing a password. It is short lived and consumed on first use.
 */
@Entity
@Table(name = "login_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginToken extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String token;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private LocalDateTime expiresAt;

  @Column(nullable = false, columnDefinition = "SMALLINT")
  @Convert(converter = BooleanToZeroOneConverter.class)
  private boolean used;
}
