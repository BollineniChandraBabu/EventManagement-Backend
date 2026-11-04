package com.familywishes.service;

import com.familywishes.dto.ThemeDtos.ThemePreferenceResponse;
import com.familywishes.dto.ThemeDtos.ThemeResponse;
import java.util.List;

public interface ThemeService {

  /** All active theme styles that the UI can offer. */
  List<ThemeResponse> listThemes();

  /** The theme selected by the signed-in user, or {@code default} when none is saved. */
  ThemePreferenceResponse getMyTheme();

  /** Saves the theme selected by the signed-in user and returns the stored value. */
  ThemePreferenceResponse saveMyTheme(String theme);
}
