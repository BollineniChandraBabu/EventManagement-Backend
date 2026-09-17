package com.familywishes.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** An administrator-managed greeting image, optionally tailored to one recipient. */
@Entity
@Table(
    name = "wish_images",
    indexes = {
      @Index(name = "idx_wish_images_event_active", columnList = "event_type,active"),
      @Index(name = "idx_wish_images_user_event", columnList = "user_id,event_type")
    })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WishImage extends ActivatableEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Null makes this image available to every recipient for the event. */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "event_type", nullable = false, length = 150)
  private String eventType;

  @Column(name = "object_key", nullable = false, unique = true, length = 1000)
  private String objectKey;
}
