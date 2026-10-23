package com.familywishes.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WishEmailExtrasTest {

  private static final String LABEL = "Open your birthday wishes";

  @Test
  void withoutALinkTheEmailIsLeftExactlyAsItWas() {
    String html = "<p>Happy birthday!</p>";

    assertSame(html, WishEmailExtras.appendWishesLink(html, null, LABEL));
    assertSame(html, WishEmailExtras.appendWishesLink(html, "  ", LABEL));
  }

  @Test
  void aLinkAddsAButtonAndAPlainTextFallbackAtTheEnd() {
    String result =
        WishEmailExtras.appendWishesLink(
            "<p>Happy birthday!</p>", "https://example.com/b/asha.html", LABEL);

    assertTrue(result.startsWith("<p>Happy birthday!</p>"));
    assertTrue(result.contains("href=\"https://example.com/b/asha.html\""));
    assertTrue(result.contains(">" + LABEL + "</a>"));
    assertTrue(result.contains("Button not working?"));
  }

  @Test
  void theBlockGoesInsideBodyWhenTheEmailIsAFullPage() {
    String result =
        WishEmailExtras.appendWishesLink(
            "<html><body><p>Hi</p></BODY></html>", "https://example.com/x", LABEL);

    int block = result.indexOf("Open your birthday wishes");
    assertTrue(block > 0 && block < result.toLowerCase().indexOf("</body>"));
    assertTrue(result.endsWith("</BODY></html>"));
  }

  @Test
  void valuesAreEscapedSoTheyCannotInjectMarkup() {
    String result =
        WishEmailExtras.appendWishesLink(
            "", "https://example.com/?a=1&b=2", "<img src=x onerror=alert(1)>");

    assertTrue(result.contains("https://example.com/?a=1&amp;b=2"));
    assertFalse(result.contains("<img"));
    assertTrue(result.contains("&lt;img"));
  }

  @Test
  void personalNotesAreEscapedAndKeepTheirLineBreaks() {
    String result =
        WishEmailExtras.appendPersonalNote("<p>Hi</p>", "Love you <b>so</b> much\nSee you soon");

    assertTrue(result.contains("Love you &lt;b&gt;so&lt;/b&gt; much<br>See you soon"));
    assertFalse(result.contains("<b>so</b>"));
  }

  @Test
  void emptyNotesChangeNothing() {
    String html = "<p>Hi</p>";

    assertSame(html, WishEmailExtras.appendPersonalNote(html, null));
    assertSame(html, WishEmailExtras.appendPersonalNote(html, "   "));
  }

  @Test
  void escapeHandlesTheFiveSpecialCharacters() {
    assertEquals("&amp;&lt;&gt;&quot;&#39;", WishEmailExtras.escape("&<>\"'"));
    assertEquals("", WishEmailExtras.escape(null));
  }
}
