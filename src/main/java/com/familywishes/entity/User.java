package com.familywishes.entity;

import com.familywishes.entity.converter.BooleanToZeroOneConverter;
import com.familywishes.entity.enums.Gender;
import com.familywishes.entity.enums.Role;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "users")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class User extends ActivatableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.ORDINAL)
  @Column(nullable = false, columnDefinition = "SMALLINT")
  @Builder.Default
  private Gender gender = Gender.MALE;

  @Enumerated(EnumType.ORDINAL)
  @Column(nullable = false, columnDefinition = "SMALLINT")
  private Role role;

  @Column(nullable = false, columnDefinition = "SMALLINT")
  @Convert(converter = BooleanToZeroOneConverter.class)
  @Builder.Default
  private boolean deleted = false;

  @Column(nullable = false)
  @Builder.Default
  private int failedLoginAttempts = 0;

  @Column(name = "mfa_enabled", nullable = false, columnDefinition = "SMALLINT")
  @Convert(converter = BooleanToZeroOneConverter.class)
  @Builder.Default
  private boolean mfaEnabled = false;

  /**
   * When false, a new sign-in revokes the user's earlier sessions so only one stays active.
   * Defaults to true (concurrent logins allowed). The DB default keeps existing rows valid on
   * migration.
   */
  @Column(
      name = "concurrent_login_allowed",
      nullable = false,
      columnDefinition = "SMALLINT DEFAULT 1")
  @Convert(converter = BooleanToZeroOneConverter.class)
  @Builder.Default
  private boolean concurrentLoginAllowed = true;

  /**
   * Identifies the user's current sign-in session. Rotated on every sign-in and carried in the
   * access token as the {@code sid} claim. When {@link #concurrentLoginAllowed} is false, a request
   * whose token {@code sid} no longer matches this value came from a superseded session and is
   * rejected so the user can sign in again. Null until the first sign-in after this field existed,
   * which keeps already-issued tokens valid.
   */
  @Column(name = "active_session_id", length = 64)
  private String activeSessionId;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "relationship_seed_id", nullable = false)
  private RelationshipSeed relationShip;

  @Column(name = "birthday")
  private LocalDate birthday;

  @Column private LocalDate lastBirthdayWishSent;

  /** Optional link to a custom birthday wishes page; added to the birthday email when present. */
  @Column(name = "birthday_wishes_url", length = 500)
  private String birthdayWishesUrl;

  @Column(columnDefinition = "SMALLINT")
  @Convert(converter = BooleanToZeroOneConverter.class)
  @Builder.Default
  private Boolean online = false;

  @Column private LocalDateTime lastSeenAt;

  @Column(name = "profile_picture_url", columnDefinition = "TEXT")
  private String profilePictureUrl;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  private UserWishSettings wishSettings;

  public boolean isOnline() {
    return Boolean.TRUE.equals(online);
  }
}
