package com.familywishes.entity;

import com.familywishes.entity.enums.Role;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role;

  @Column(nullable = false)
  @Builder.Default
  private boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private boolean deleted = false;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "relationship_seed_id", nullable = false)
  private RelationshipSeed relationShip;

  @Column(name = "birthday")
  private LocalDate birthday;

  @Column private LocalDate lastBirthdayWishSent;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
  private UserWishSettings wishSettings;

  @PrePersist
  void init() {
    if (createdAt == null) createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
  }
}
