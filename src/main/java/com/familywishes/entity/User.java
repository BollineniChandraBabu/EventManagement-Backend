package com.familywishes.entity;

import com.familywishes.entity.converter.BooleanToZeroOneConverter;
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
  private Role role;

  @Column(nullable = false, columnDefinition = "SMALLINT")
  @Convert(converter = BooleanToZeroOneConverter.class)
  @Builder.Default
  private boolean deleted = false;

  @Column(nullable = false)
  @Builder.Default
  private int failedLoginAttempts = 0;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "relationship_seed_id", nullable = false)
  private RelationshipSeed relationShip;

  @Column(name = "birthday")
  private LocalDate birthday;

  @Column private LocalDate lastBirthdayWishSent;

  @Column(columnDefinition = "SMALLINT")
  @Convert(converter = BooleanToZeroOneConverter.class)
  @Builder.Default
  private Boolean online = false;

  @Column private LocalDateTime lastSeenAt;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  private UserWishSettings wishSettings;

  public boolean isOnline() {
    return Boolean.TRUE.equals(online);
  }

}
