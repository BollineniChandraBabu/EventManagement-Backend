package com.familywishes.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_type_seeds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTypeSeed {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String displayName;

  @Column(nullable = false)
  private boolean active;
}
