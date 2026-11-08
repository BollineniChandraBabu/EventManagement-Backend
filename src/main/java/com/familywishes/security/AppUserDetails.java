package com.familywishes.security;

import java.util.Collection;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

/**
 * The authenticated principal. Extends Spring's {@code User} so it can still back the {@code
 * DaoAuthenticationProvider}, and adds the fields the JWT filter needs to police single-session
 * sign-in.
 */
@Getter
public class AppUserDetails extends org.springframework.security.core.userdetails.User {
  private final Long userId;
  private final boolean concurrentLoginAllowed;
  private final String activeSessionId;

  public AppUserDetails(
      Long userId,
      String username,
      String password,
      boolean enabled,
      boolean concurrentLoginAllowed,
      String activeSessionId,
      Collection<? extends GrantedAuthority> authorities) {
    super(username, password, enabled, true, true, true, authorities);
    this.userId = userId;
    this.concurrentLoginAllowed = concurrentLoginAllowed;
    this.activeSessionId = activeSessionId;
  }
}
