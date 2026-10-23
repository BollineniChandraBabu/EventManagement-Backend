package com.familywishes.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WishImageKeysTest {

  @Test
  void aBirthdayLooksForBirthdayImages() {
    assertEquals(List.of("Birthday"), WishImageKeys.forWish("Birthday", ""));
  }

  @Test
  void aFestivalLooksForItsOwnImageThenForTheGeneralFestivalImage() {
    assertEquals(List.of("Diwali", "Festival"), WishImageKeys.forWish("", "Diwali"));
  }

  @Test
  void eventCodesAlsoMatchImagesNamedAfterTheirDisplayName() {
    assertEquals(
        List.of("WEDDING_ANNIVERSARY", "WEDDING ANNIVERSARY"),
        WishImageKeys.forWish("WEDDING_ANNIVERSARY", null));
  }

  @Test
  void nothingIsRepeatedAndBlankValuesAreIgnored() {
    assertEquals(List.of("festival"), WishImageKeys.forWish("", "festival"));
    assertTrue(WishImageKeys.forWish(null, null).isEmpty());
    assertTrue(WishImageKeys.forWish("  ", "  ").isEmpty());
  }
}
