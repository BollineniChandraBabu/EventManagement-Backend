package com.familywishes.entity;

import com.familywishes.config.CacheNames;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = CacheNames.SEED_DATA)
@Table(name = "event_type_seeds")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EventTypeSeed extends ActivatableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String code;

  @Column(nullable = false)
  private String displayName;
}
