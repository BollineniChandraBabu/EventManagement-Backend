package com.familywishes.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.auth.oauth2.UserCredentials;
import java.io.IOException;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class GmailConfig {

  private static final String APPLICATION_NAME = "Family Wishes";

  @Value("${gmail.client-id}")
  private String clientId;

  @Value("${gmail.client-secret}")
  private String clientSecret;

  @Value("${gmail.refresh-token}")
  private String refreshToken;

  @Bean
  public Gmail gmailService() throws Exception {

    UserCredentials credentials =
        UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRefreshToken(refreshToken)
            .build();

    return new Gmail.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JacksonFactory.getDefaultInstance(),
            request -> {
              request.getHeaders().setAuthorization("Bearer " + resolveAccessToken(credentials));
            })
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  String resolveAccessToken(UserCredentials credentials) throws IOException {
    try {
      credentials.refreshIfExpired();
      return credentials.getAccessToken().getTokenValue();
    } catch (IOException ex) {
      if (isInvalidGrant(ex)) {
        log.error(
            "Gmail OAuth refresh token is expired or revoked. Generate a new refresh token and update GMAIL_REFRESH_TOKEN.");
        throw new IllegalStateException(
            "Gmail OAuth refresh token is expired or revoked. Re-authorize the Gmail app and update GMAIL_REFRESH_TOKEN.",
            ex);
      }
      throw ex;
    }
  }

  boolean isInvalidGrant(IOException ex) {
    HttpResponseException responseException = extractHttpResponseException(ex);
    if (responseException == null) {
      return false;
    }

    String content = responseException.getContent();
    if (content == null) {
      return false;
    }

    String normalized = content.toLowerCase(Locale.ROOT);
    return normalized.contains("invalid_grant")
        && (normalized.contains("expired") || normalized.contains("revoked"));
  }

  HttpResponseException extractHttpResponseException(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof HttpResponseException responseException) {
        return responseException;
      }
      current = current.getCause();
    }
    return null;
  }
}
