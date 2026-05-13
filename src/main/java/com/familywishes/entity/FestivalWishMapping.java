package com.familywishes.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "festival_wish_mappings",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_festival_user_mapping",
            columnNames = {"special_event_id", "user_id"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FestivalWishMapping extends ActivatableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "special_event_id", nullable = false)
  private SpecialEvent specialEvent;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(length = 1000)
  private String customMessage;

  private LocalDate lastWishSentOn;
}
