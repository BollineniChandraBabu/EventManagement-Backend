package com.familywishes.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "email_templates")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EmailTemplate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String subject;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String htmlContent;
    @Column(nullable = false)
    private Integer version;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        updatedAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata")); }
}
