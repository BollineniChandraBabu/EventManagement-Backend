package com.familywishes.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.auth.oauth2.GoogleAuthException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class GmailConfigTest {

  private final GmailConfig gmailConfig = new GmailConfig();

  @Test
  void isInvalidGrantShouldReturnTrueWhenTokenRevokedMessagePresent() throws Exception {
    GoogleAuthException authException =
        GoogleAuthException.createWithTokenEndpointResponseException(
            buildHttpResponseException(
                "{\"error\":\"invalid_grant\",\"error_description\":\"Token has been expired or revoked.\"}"));

    assertTrue(gmailConfig.isInvalidGrant(authException));
  }

  @Test
  void isInvalidGrantShouldReturnFalseWhenResponseDoesNotContainRevokedOrExpired()
      throws Exception {
    GoogleAuthException authException =
        GoogleAuthException.createWithTokenEndpointResponseException(
            buildHttpResponseException(
                "{\"error\":\"invalid_grant\",\"error_description\":\"Bad Request\"}"));

    assertFalse(gmailConfig.isInvalidGrant(authException));
  }

  private HttpResponseException buildHttpResponseException(String responseContent)
      throws IOException {
    HttpTransport transport = new MockHttpTransport();
    var requestFactory = transport.createRequestFactory();
    var request =
        requestFactory.buildGetRequest(
            new com.google.api.client.http.GenericUrl("https://oauth2.googleapis.com/token"));

    return new HttpResponseException.Builder(400, "Bad Request", request.getHeaders())
        .setContent(responseContent)
        .build();
  }
}
