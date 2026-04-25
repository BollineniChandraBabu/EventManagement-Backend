package com.familywishes.util;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplateBuilder {
  public String build(String htmlBody, String signatureImageUrl) {
    String content = extractBodyContent(htmlBody);
    String signatureBlock = buildSignatureBlock(signatureImageUrl);

    return """
        <!DOCTYPE html>
        <html lang="en">
          <body style="margin:0;padding:24px;background:#f3f4f6;font-family:Arial,sans-serif;color:#111827;">
            <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%" style="max-width:680px;margin:0 auto;background:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #e5e7eb;">
              <tr>
                <td style="padding:20px 24px;background:linear-gradient(90deg,#fff7ed,#ecfeff);border-bottom:1px solid #e5e7eb;">
                  <h2 style="margin:0;font-size:20px;font-weight:700;color:#1f2937;">Golden Greetings</h2>
                  <p style="margin:6px 0 0 0;font-size:13px;color:#6b7280;">Automated wishes with care</p>
                </td>
              </tr>
              <tr>
                <td style="padding:24px;font-size:15px;line-height:1.65;">
                  %s
                </td>
              </tr>
              <tr>
                <td style="padding:0 24px 22px 24px;">
                  %s
                </td>
              </tr>
            </table>
          </body>
        </html>
        """
        .formatted(content, signatureBlock);
  }

  private String extractBodyContent(String htmlBody) {
    if (htmlBody == null || htmlBody.isBlank()) {
      return "<p style='margin:0'>Hello,</p>";
    }
    String normalized = htmlBody.trim();
    normalized = normalized.replaceAll("(?is)<!doctype[^>]*>", "");
    normalized = normalized.replaceAll("(?is)<html[^>]*>", "");
    normalized = normalized.replaceAll("(?is)</html>", "");
    normalized = normalized.replaceAll("(?is)<body[^>]*>", "");
    normalized = normalized.replaceAll("(?is)</body>", "");
    return normalized.trim();
  }

  private String buildSignatureBlock(String signatureImageUrl) {
    String imageHtml =
        signatureImageUrl == null || signatureImageUrl.isBlank()
            ? ""
            : "<img src='"
                + signatureImageUrl
                + "' alt='DailyWishSender signature' style='max-width:180px;width:100%;height:auto;display:block;margin:0 auto 12px auto;' />";

    return """
        <div style="margin-top:8px;padding-top:16px;border-top:1px solid #e5e7eb;text-align:center;">
          %s
          <p style="margin:0;font-size:12px;color:#9ca3af;">Thanks and regards,<br/>Golden Greetings Team</p>
        </div>
        """
        .formatted(imageHtml);
  }
}
