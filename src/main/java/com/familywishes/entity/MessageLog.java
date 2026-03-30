package com.familywishes.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@RequiredArgsConstructor
@Data
public class MessageLog extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String instagramUserId;

  @Column(length = 1000)
  private String message;

  @Column(nullable = false, columnDefinition = "SMALLINT")
  @Enumerated(EnumType.ORDINAL)
  private MessageStatus status;

  @Column(nullable = false, columnDefinition = "SMALLINT")
  @Enumerated(EnumType.ORDINAL)
  private MessageType messageType;

  private int retryCount;
  private LocalDateTime lastAttemptTime;
  private String errorMessage;
}
