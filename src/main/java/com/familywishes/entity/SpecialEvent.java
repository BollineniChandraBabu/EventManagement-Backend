package com.familywishes.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "seed_special_events")
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
  private LocalDate eventDate;

  @Column(length = 1000)
  private String message;

  @Builder.Default private boolean active = true;
}
