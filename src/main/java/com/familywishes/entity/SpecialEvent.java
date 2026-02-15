package com.familywishes.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecialEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventName;
    private int day;
    private int month;

    @Column(length = 1000)
    private String message;
    @Builder.Default
    private boolean active = true;
}
