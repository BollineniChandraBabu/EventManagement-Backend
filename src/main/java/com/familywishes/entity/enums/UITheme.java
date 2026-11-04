package com.familywishes.entity.enums;

/**
 * Canonical UI theme styles seeded into {@code theme_seeds}. Each style has a technical name
 * ({@code code}, used by the UI) and a user-friendly name ({@code displayName}). {@link #DEFAULT}
 * is used when a user has not chosen one.
 */
public enum UITheme {
  SKEUOMORPHISM("skeuomorphism", "Realistic Design"),
  NEOMORPHISM("neomorphism", "Soft UI"),
  GLASSMORPHISM("glassmorphism", "Frosted Glass"),
  CLAYMORPHISM("claymorphism", "Soft 3D Design"),
  MINIMALISM("minimalism", "Clean & Simple"),
  MAXIMALISM("maximalism", "Bold & Vibrant"),
  BRUTALISM("brutalism", "Bold & Raw"),
  LIQUID_GLASS("liquid-glass", "Liquid Glass"),
  BENTO_GRID("bento-grid", "Modular Grid"),
  SPATIAL_UI("spatial-ui", "3D Spatial Design"),
  DEFAULT("default", "default");

  private final String code;
  private final String displayName;

  UITheme(String code, String displayName) {
    this.code = code;
    this.displayName = displayName;
  }

  public String getCode() {
    return code;
  }

  public String getDisplayName() {
    return displayName;
  }
}
