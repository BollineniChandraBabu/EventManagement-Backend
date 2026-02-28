package com.familywishes.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/")
public class GoogleOAuthController {

  @Value("${gmail.client-id}")
  private String clientId;

  @Value("${gmail.client-secret}")
  private String clientSecret;

  @Value("${gmail.redirect-uri}")
  private String redirectUri;

  private final RestTemplate restTemplate = new RestTemplate();

  // STEP 1 - Redirect user to Google Login
  @GetMapping("/google/login")
  public void redirectToGoogle(HttpServletResponse response) throws IOException {

    String url =
        "https://accounts.google.com/o/oauth2/v2/auth"
            + "?client_id="
            + clientId
            + "&redirect_uri="
            + redirectUri
            + "&response_type=code"
            + "&scope=https://www.googleapis.com/auth/gmail.send"
            + "&access_type=offline"
            + "&prompt=consent";

    response.sendRedirect(url);
  }

  // STEP 2 - Handle Callback and Exchange Code for Token
  @GetMapping("/oauth2callback")
  public ResponseEntity<?> handleGoogleCallback(@RequestParam("code") String code) {

    String tokenUrl = "https://oauth2.googleapis.com/token";

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("code", code);
    params.add("client_id", clientId);
    params.add("client_secret", clientSecret);
    params.add("redirect_uri", redirectUri);
    params.add("grant_type", "authorization_code");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

    ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

    return ResponseEntity.ok(response.getBody());
  }

  // STEP 3 - Send Email using Access Token
  @PostMapping("/gmail/send")
  public ResponseEntity<?> sendEmail(@RequestParam String accessToken) {

    String gmailUrl = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

    String rawEmail =
        "From: me\r\n"
            + "To: chandra@yopmail.com\r\n"
            + "Subject: Test Email from Render\r\n\r\n"
            + "Hello from Spring Boot on Render!";

    String encodedEmail = Base64.getUrlEncoder().encodeToString(rawEmail.getBytes());

    Map<String, String> body = new HashMap<>();
    body.put("raw", encodedEmail);

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

    ResponseEntity<String> response = restTemplate.postForEntity(gmailUrl, request, String.class);

    return ResponseEntity.ok(response.getBody());
  }

  // STEP 4 - Refresh Access Token
  @PostMapping("/gmail/refresh")
  public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {

    String tokenUrl = "https://oauth2.googleapis.com/token";

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("client_id", clientId);
    params.add("client_secret", clientSecret);
    params.add("refresh_token", refreshToken);
    params.add("grant_type", "refresh_token");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

    ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

    return ResponseEntity.ok(response.getBody());
  }
}
