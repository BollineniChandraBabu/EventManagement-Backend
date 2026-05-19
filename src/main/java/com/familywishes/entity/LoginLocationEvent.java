package com.familywishes.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "login_location_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLocationEvent extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "login_location", nullable = false, length = 255)
  private String loginLocation;

  @Column(name = "logged_in_at", nullable = false)
  private LocalDateTime loggedInAt;
}
