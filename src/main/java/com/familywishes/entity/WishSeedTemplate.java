package com.familywishes.entity;

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

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "type_seed_id", nullable = false, unique = true)
  private TemplateTypeSeed type;

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
