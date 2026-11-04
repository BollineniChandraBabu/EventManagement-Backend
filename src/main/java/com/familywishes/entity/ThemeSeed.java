package com.familywishes.entity;

import com.familywishes.config.CacheNames;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/** A selectable UI theme style, kept in the database so it can be managed like other seed data. */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = CacheNames.SEED_DATA)
@Table(name = "theme_seeds")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ThemeSeed extends ActivatableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String displayName;
}
