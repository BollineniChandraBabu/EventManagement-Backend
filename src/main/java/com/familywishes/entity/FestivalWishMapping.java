package com.familywishes.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(
    name = "festival_wish_mappings",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_festival_user_mapping",
            columnNames = {"special_event_id", "user_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FestivalWishMapping {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "special_event_id", nullable = false)
  private SpecialEvent specialEvent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(length = 1000)
  private String customMessage;

  private LocalDate lastWishSentOn;

  @Builder.Default private boolean active = true;
}
