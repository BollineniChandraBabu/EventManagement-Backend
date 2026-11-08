package com.familywishes.entity;

import com.familywishes.entity.enums.CircleStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * A circle request / accepted circle between two users. One row holds the pair, the requester and
 * the addressee, so the direction of the request is known until it is accepted.
 */
@Entity
@Table(
    name = "circles",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_circle_pair",
            columnNames = {"requester_id", "addressee_id"}),
    indexes = {
      @Index(name = "idx_circles_requester", columnList = "requester_id"),
      @Index(name = "idx_circles_addressee", columnList = "addressee_id")
    })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Circle extends AuditableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "requester_id", nullable = false)
  private User requester;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "addressee_id", nullable = false)
  private User addressee;

  @Enumerated(EnumType.ORDINAL)
  @Column(nullable = false, columnDefinition = "SMALLINT")
  @Builder.Default
  private CircleStatus status = CircleStatus.PENDING;

  @Column private LocalDateTime respondedAt;
}
