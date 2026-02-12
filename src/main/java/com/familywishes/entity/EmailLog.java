package com.familywishes.entity;

import com.familywishes.entity.enums.EmailStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String recipientEmail;
    @Column(nullable = false)
    private String subject;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status;
    @Column(nullable = false)
    private int retryCount;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    private LocalDateTime sentAt;
}
