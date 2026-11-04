package com.familywishes.controller;

import com.familywishes.dto.ThemeDtos.ThemePreferenceRequest;
import com.familywishes.dto.ThemeDtos.ThemePreferenceResponse;
import com.familywishes.dto.ThemeDtos.ThemeResponse;
import com.familywishes.service.ThemeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/themes")
@RequiredArgsConstructor
public class ThemeController {
  private final ThemeService themeService;

  /** All available theme styles (technical name + user-friendly name). */
  @GetMapping
  public List<ThemeResponse> listThemes() {
    return themeService.listThemes();
  }

  /** The signed-in user's selected theme; falls back to {@code default}. */
  @GetMapping("/me")
  public ThemePreferenceResponse myTheme() {
    return themeService.getMyTheme();
  }

  /** Saves the signed-in user's selected theme. */
  @PutMapping("/me")
  public ThemePreferenceResponse saveMyTheme(@Valid @RequestBody ThemePreferenceRequest request) {
    return themeService.saveMyTheme(request.theme());
  }
}
