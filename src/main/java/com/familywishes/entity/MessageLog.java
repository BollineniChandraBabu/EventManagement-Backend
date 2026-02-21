package com.familywishes.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@RequiredArgsConstructor
@Data
public class MessageLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String instagramUserId;

  @Column(length = 1000)
  private String message;

  @Enumerated(EnumType.STRING)
  private MessageStatus status;

  @Enumerated(EnumType.STRING)
  private MessageType messageType;

  private int retryCount;
  private LocalDateTime createdAt;
  private LocalDateTime lastAttemptTime;
  private String errorMessage;
}
