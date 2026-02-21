package com.familywishes.entity;

import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.entity.enums.EmailType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

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

    @Enumerated(EnumType.STRING)
    @Column
    private EmailType emailType;
    @Column(nullable = false)
    private int retryCount;
    @Column(columnDefinition = "TEXT")
    private String body;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    private LocalDateTime sentAt;

    @JdbcTypeCode(java.sql.Types.VARBINARY)
    @Column(name = "image_data")
    private byte[] imageData;

    @PrePersist
    @PreUpdate
    private void ensureEmailType() {
        if (emailType == null) {
            emailType = EmailType.EVENT;
        }
    }
}
