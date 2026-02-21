package com.familywishes.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_wish_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWishSettings {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private boolean goodMorningEnabled;

  private boolean goodNightEnabled;

  private boolean birthdayEnabled;

  @OneToOne
  @JoinColumn(name = "user_id")
  private User user;
}
