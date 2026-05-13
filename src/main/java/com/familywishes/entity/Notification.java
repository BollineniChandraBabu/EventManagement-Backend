package com.familywishes.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 150)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String message;

  @Column(nullable = false)
  @Builder.Default
  private Boolean published = false;

  @Column(nullable = false)
  @Builder.Default
  private Boolean canSendEmail = false;

  private LocalDateTime scheduledFrom;

  private LocalDateTime scheduledTo;

  private LocalDateTime publishedAt;

  @Column(nullable = false, updatable = false)
  private String createdBy;

  @Column(nullable = false)
  private String updatedBy;
}
