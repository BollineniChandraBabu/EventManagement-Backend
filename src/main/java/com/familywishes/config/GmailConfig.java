package com.familywishes.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.auth.oauth2.UserCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
              credentials.refreshIfExpired();
              request
                  .getHeaders()
                  .setAuthorization("Bearer " + credentials.getAccessToken().getTokenValue());
            })
        .setApplicationName(APPLICATION_NAME)
        .build();
  }
}
