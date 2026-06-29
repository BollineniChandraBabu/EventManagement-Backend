package com.familywishes.entity;

import com.familywishes.entity.converter.BooleanToZeroOneConverter;
import com.familywishes.entity.enums.Gender;
import com.familywishes.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "violated_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ViolatedUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(name = "login_location", nullable = false, length = 255)
  private String loginLocation;

  @Column(name = "logged_in_at", nullable = false)
  private LocalDateTime loggedInAt;

  @Column(name = "ip_address", length = 64)
  private String ipAddress;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;
}
