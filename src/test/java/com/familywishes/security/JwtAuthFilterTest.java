package com.familywishes.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

  @Mock private JwtService jwtService;
  @Mock private UserDetailsService userDetailsService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private AppUserDetails details(boolean concurrentAllowed, String activeSessionId) {
    return new AppUserDetails(
        1L,
        "user@example.com",
        "pw",
        true,
        concurrentAllowed,
        activeSessionId,
        List.of(new SimpleGrantedAuthority("ROLE_USER")));
  }

  private MockHttpServletRequest request() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer token");
    return request;
  }

  @Test
  void rejectsAStaleSessionWhenConcurrentLoginIsDisallowed() throws Exception {
    when(jwtService.isValid("token")).thenReturn(true);
    when(jwtService.extractSubject("token")).thenReturn("user@example.com");
    when(jwtService.extractSessionId("token")).thenReturn("old-session");
    when(userDetailsService.loadUserByUsername("user@example.com"))
        .thenReturn(details(false, "new-session"));

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    new JwtAuthFilter(jwtService, userDetailsService).doFilter(request(), response, chain);

    assertEquals(401, response.getStatus());
    assertNotNull(response.getContentAsString());
    assertNull(chain.getRequest(), "the request must not reach the chain");
  }

  @Test
  void allowsTheCurrentSessionWhenConcurrentLoginIsDisallowed() throws Exception {
    when(jwtService.isValid("token")).thenReturn(true);
    when(jwtService.extractSubject("token")).thenReturn("user@example.com");
    when(jwtService.extractSessionId("token")).thenReturn("current-session");
    when(userDetailsService.loadUserByUsername("user@example.com"))
        .thenReturn(details(false, "current-session"));

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    new JwtAuthFilter(jwtService, userDetailsService).doFilter(request(), response, chain);

    assertEquals(200, response.getStatus());
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void allowsAnySessionWhenConcurrentLoginIsAllowed() throws Exception {
    when(jwtService.isValid("token")).thenReturn(true);
    when(jwtService.extractSubject("token")).thenReturn("user@example.com");
    when(userDetailsService.loadUserByUsername("user@example.com"))
        .thenReturn(details(true, "new-session"));

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    new JwtAuthFilter(jwtService, userDetailsService).doFilter(request(), response, chain);

    assertEquals(200, response.getStatus());
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void allowsTokensIssuedBeforeSessionTrackingExisted() throws Exception {
    when(jwtService.isValid("token")).thenReturn(true);
    when(jwtService.extractSubject("token")).thenReturn("user@example.com");
    when(userDetailsService.loadUserByUsername("user@example.com"))
        .thenReturn(details(false, null));

    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    new JwtAuthFilter(jwtService, userDetailsService).doFilter(request(), response, chain);

    assertEquals(200, response.getStatus());
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void ignoresRequestsWithoutABearerToken() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();
    new JwtAuthFilter(jwtService, userDetailsService).doFilter(request, response, chain);

    assertEquals(200, response.getStatus());
    verify(userDetailsService, never()).loadUserByUsername(any());
  }
}
