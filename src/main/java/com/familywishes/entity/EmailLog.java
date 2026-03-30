package com.familywishes.entity;

import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.entity.enums.EmailType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "email_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String recipientEmail;

  @Column(nullable = false)
  private String subject;

  @Enumerated(EnumType.ORDINAL)
  @Column(nullable = false, columnDefinition = "SMALLINT")
  private EmailStatus status;

  @Enumerated(EnumType.ORDINAL)
  @Column(columnDefinition = "SMALLINT")
  private EmailType emailType;

  @Column(nullable = false)
  private int retryCount;

  @Column(columnDefinition = "TEXT")
  private String body;

  @Column(columnDefinition = "TEXT")
  private String errorMessage;

  private LocalDateTime sentAt;

  @Column(name = "image_url", columnDefinition = "TEXT")
  private String imageUrl;

  @PrePersist
  @PreUpdate
  private void ensureEmailType() {
    if (emailType == null) {
      emailType = EmailType.EVENT;
    }
  }
}
