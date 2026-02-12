package com.familywishes.entity;

import com.familywishes.entity.enums.EventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "events")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;
    private String festivalName;
    @Column(nullable = false)
    private LocalDate eventDate;
    @Column(nullable = false)
    private boolean recurring;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
