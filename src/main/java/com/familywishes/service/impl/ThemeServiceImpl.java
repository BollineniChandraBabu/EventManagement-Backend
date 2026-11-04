package com.familywishes.service.impl;

import com.familywishes.dto.ThemeDtos.ThemePreferenceResponse;
import com.familywishes.dto.ThemeDtos.ThemeResponse;
import com.familywishes.entity.ThemeSeed;
import com.familywishes.entity.User;
import com.familywishes.entity.UserThemePreference;
import com.familywishes.entity.enums.UITheme;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.ThemeSeedRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.repository.UserThemePreferenceRepository;
import com.familywishes.service.ThemeService;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThemeServiceImpl implements ThemeService {
  private final ThemeSeedRepository themeSeedRepository;
  private final UserThemePreferenceRepository userThemePreferenceRepository;
  private final UserRepository userRepository;

  @Override
  public List<ThemeResponse> listThemes() {
    return themeSeedRepository.findByActiveTrueOrderByDisplayNameAsc().stream()
        .map(seed -> new ThemeResponse(seed.getId(), seed.getCode(), seed.getDisplayName()))
        .toList();
  }

  @Override
  public ThemePreferenceResponse getMyTheme() {
    User user = findAuthenticatedUser();
    String theme =
        userThemePreferenceRepository
            .findByUserId(user.getId())
            .map(UserThemePreference::getTheme)
            .orElse(UITheme.DEFAULT.getCode());
    return new ThemePreferenceResponse(theme);
  }

  @Override
  @Transactional
  public ThemePreferenceResponse saveMyTheme(String theme) {
    String code = theme == null ? "" : theme.trim().toLowerCase(Locale.ROOT);
    // The theme must be one the DB exposes, otherwise the UI could show a value it cannot render.
    ThemeSeed seed =
        themeSeedRepository
            .findByCodeAndActiveTrue(code)
            .orElseThrow(() -> new BadRequestException("Unsupported theme: " + theme));

    User user = findAuthenticatedUser();
    UserThemePreference preference =
        userThemePreferenceRepository
            .findByUserId(user.getId())
            .orElseGet(() -> UserThemePreference.builder().user(user).build());
    preference.setTheme(seed.getCode());
    userThemePreferenceRepository.save(preference);
    return new ThemePreferenceResponse(seed.getCode());
  }

  private User findAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new NotFoundException("Authenticated user not found");
    }

    return userRepository
        .findByEmailAndDeletedFalse(authentication.getName())
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
