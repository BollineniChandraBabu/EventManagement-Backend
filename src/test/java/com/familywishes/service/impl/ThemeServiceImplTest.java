package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familywishes.dto.ThemeDtos.ThemePreferenceResponse;
import com.familywishes.dto.ThemeDtos.ThemeResponse;
import com.familywishes.entity.ThemeSeed;
import com.familywishes.entity.User;
import com.familywishes.entity.UserThemePreference;
import com.familywishes.entity.enums.UITheme;
import com.familywishes.exception.BadRequestException;
import com.familywishes.repository.ThemeSeedRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.repository.UserThemePreferenceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ThemeServiceImplTest {

  @Mock private ThemeSeedRepository themeSeedRepository;
  @Mock private UserThemePreferenceRepository userThemePreferenceRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private ThemeServiceImpl themeService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(String email) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                email, "password", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    when(userRepository.findByEmailAndDeletedFalse(email))
        .thenReturn(Optional.of(User.builder().id(7L).email(email).build()));
  }

  @Test
  void listThemesReturnsTechnicalAndUserFriendlyNamesFromDatabase() {
    when(themeSeedRepository.findByActiveTrueOrderByDisplayNameAsc())
        .thenReturn(
            List.of(
                ThemeSeed.builder()
                    .id(1L)
                    .code("bento-grid")
                    .displayName("Modular Grid")
                    .active(true)
                    .build(),
                ThemeSeed.builder()
                    .id(2L)
                    .code("default")
                    .displayName("default")
                    .active(true)
                    .build()));

    List<ThemeResponse> themes = themeService.listThemes();

    assertEquals(2, themes.size());
    assertEquals("bento-grid", themes.get(0).code());
    assertEquals("Modular Grid", themes.get(0).displayName());
    assertEquals("default", themes.get(1).code());
    assertEquals("default", themes.get(1).displayName());
  }

  @Test
  void getMyThemeFallsBackToDefaultWhenNothingIsSaved() {
    authenticateAs("ravi@example.com");
    when(userThemePreferenceRepository.findByUserId(7L)).thenReturn(Optional.empty());

    ThemePreferenceResponse response = themeService.getMyTheme();

    assertEquals(UITheme.DEFAULT.getCode(), response.theme());
  }

  @Test
  void getMyThemeReturnsSavedTheme() {
    authenticateAs("ravi@example.com");
    when(userThemePreferenceRepository.findByUserId(7L))
        .thenReturn(
            Optional.of(
                UserThemePreference.builder().id(3L).theme("liquid-glass").user(null).build()));

    ThemePreferenceResponse response = themeService.getMyTheme();

    assertEquals("liquid-glass", response.theme());
  }

  @Test
  void saveMyThemePersistsSelectedThemeForExistingPreference() {
    authenticateAs("ravi@example.com");
    UserThemePreference existing =
        UserThemePreference.builder().id(3L).theme("default").user(null).build();
    when(themeSeedRepository.findByCodeAndActiveTrue("glassmorphism"))
        .thenReturn(
            Optional.of(
                ThemeSeed.builder()
                    .id(5L)
                    .code("glassmorphism")
                    .displayName("Frosted Glass")
                    .active(true)
                    .build()));
    when(userThemePreferenceRepository.findByUserId(7L)).thenReturn(Optional.of(existing));
    when(userThemePreferenceRepository.save(any(UserThemePreference.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ThemePreferenceResponse response = themeService.saveMyTheme("GLASSMORPHISM");

    assertEquals("glassmorphism", response.theme());
    ArgumentCaptor<UserThemePreference> captor = ArgumentCaptor.forClass(UserThemePreference.class);
    verify(userThemePreferenceRepository).save(captor.capture());
    assertEquals("glassmorphism", captor.getValue().getTheme());
    assertEquals(3L, captor.getValue().getId());
  }

  @Test
  void saveMyThemeCreatesPreferenceWhenMissing() {
    authenticateAs("ravi@example.com");
    when(themeSeedRepository.findByCodeAndActiveTrue("bento-grid"))
        .thenReturn(
            Optional.of(
                ThemeSeed.builder()
                    .id(6L)
                    .code("bento-grid")
                    .displayName("Modular Grid")
                    .active(true)
                    .build()));
    when(userThemePreferenceRepository.findByUserId(7L)).thenReturn(Optional.empty());
    when(userThemePreferenceRepository.save(any(UserThemePreference.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    ThemePreferenceResponse response = themeService.saveMyTheme("bento-grid");

    assertEquals("bento-grid", response.theme());
  }

  @Test
  void saveMyThemeRejectsUnknownTheme() {
    when(themeSeedRepository.findByCodeAndActiveTrue("neon")).thenReturn(Optional.empty());

    assertThrows(BadRequestException.class, () -> themeService.saveMyTheme("neon"));

    verify(userThemePreferenceRepository, never()).save(any());
  }

  @Test
  void saveMyThemeRejectsThemeNotExposedByDatabase() {
    when(themeSeedRepository.findByCodeAndActiveTrue("claymorphism")).thenReturn(Optional.empty());

    assertThrows(BadRequestException.class, () -> themeService.saveMyTheme("claymorphism"));

    verify(userThemePreferenceRepository, never()).save(any());
  }
}
