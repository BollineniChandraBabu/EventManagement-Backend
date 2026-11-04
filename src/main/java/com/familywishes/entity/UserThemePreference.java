package com.familywishes.entity;

import com.familywishes.entity.enums.UITheme;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** The UI theme a user has selected. Defaults to {@link UITheme#DEFAULT}. */
@Entity
@Table(name = "user_theme_preferences")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserThemePreference extends AuditableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  @Builder.Default
  private String theme = UITheme.DEFAULT.getCode();

  @OneToOne
  @JoinColumn(name = "user_id", unique = true)
  private User user;
}
