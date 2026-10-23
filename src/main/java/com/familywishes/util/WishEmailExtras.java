package com.familywishes.util;

import java.util.Locale;

/**
 * Small, email-safe HTML blocks added to the end of a wish email: a "wishes page" button and a
 * personal note. Everything that comes from a user is escaped, so it can never inject markup.
 */
public final class WishEmailExtras {

  private static final String BUTTON_COLOR = "#6d28d9";
  private static final String FONT = "font-family:Arial,Helvetica,sans-serif;";

  private WishEmailExtras() {}

  /** Adds a button linking to the wishes page. Returns the email unchanged when there is no link. */
  public static String appendWishesLink(String html, String url, String label) {
    if (url == null || url.isBlank()) {
      return html;
    }
    String safeUrl = escape(url.trim());
    String block =
        "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\""
            + " style=\"margin:24px auto 8px;\"><tr><td align=\"center\" bgcolor=\""
            + BUTTON_COLOR
            + "\" style=\"border-radius:8px;\"><a href=\""
            + safeUrl
            + "\" target=\"_blank\" rel=\"noopener noreferrer\" style=\"display:inline-block;"
            + "padding:12px 24px;"
            + FONT
            + "font-size:16px;font-weight:bold;color:#ffffff;text-decoration:none;border-radius:8px;\">"
            + escape(label)
            + "</a></td></tr></table>"
            + "<p style=\"text-align:center;margin:4px 0 0;"
            + FONT
            + "font-size:12px;color:#6b7280;\">Button not working? Open this link: <a href=\""
            + safeUrl
            + "\" target=\"_blank\" rel=\"noopener noreferrer\" style=\"color:"
            + BUTTON_COLOR
            + ";\">"
            + safeUrl
            + "</a></p>";
    return insertAtEnd(html, block);
  }

  /** Adds a short personal note. Returns the email unchanged when the note is empty. */
  public static String appendPersonalNote(String html, String note) {
    if (note == null || note.isBlank()) {
      return html;
    }
    String text = escape(note.trim()).replace("\r\n", "\n").replace("\n", "<br>");
    String block =
        "<div style=\"margin:20px auto 0;max-width:520px;padding:12px 16px;border-left:4px solid "
            + BUTTON_COLOR
            + ";background:#f5f3ff;"
            + FONT
            + "font-size:15px;line-height:1.5;color:#374151;\">"
            + text
            + "</div>";
    return insertAtEnd(html, block);
  }

  /** Puts the block before {@code </body>} when the email is a full page, else at the very end. */
  private static String insertAtEnd(String html, String block) {
    String source = html == null ? "" : html;
    int bodyEnd = source.toLowerCase(Locale.ROOT).lastIndexOf("</body>");
    if (bodyEnd < 0) {
      return source + block;
    }
    return source.substring(0, bodyEnd) + block + source.substring(bodyEnd);
  }

  static String escape(String value) {
    if (value == null) {
      return "";
    }
    StringBuilder out = new StringBuilder(value.length() + 16);
    for (char c : value.toCharArray()) {
      switch (c) {
        case '&' -> out.append("&amp;");
        case '<' -> out.append("&lt;");
        case '>' -> out.append("&gt;");
        case '"' -> out.append("&quot;");
        case '\'' -> out.append("&#39;");
        default -> out.append(c);
      }
    }
    return out.toString();
  }
}
