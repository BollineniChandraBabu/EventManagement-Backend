package com.familywishes.entity;

import com.familywishes.entity.enums.SeedTemplateType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seed_wish_templates")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishSeedTemplate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, unique = true)
  private SeedTemplateType type;

  @Column(nullable = false)
  private String relation;

  @Column(nullable = false)
  private String event;

  @Column(nullable = false)
  private String tone;

  @Column(nullable = false)
  private String language;

  @Column(nullable = false)
  private boolean active;
}
