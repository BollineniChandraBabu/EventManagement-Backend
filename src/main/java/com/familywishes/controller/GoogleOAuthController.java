package com.familywishes.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/")
public class GoogleOAuthController {

  @Value("${gmail.client-id}")
  private String clientId;

  @Value("${gmail.client-secret}")
  private String clientSecret;

  private final RestTemplate restTemplate = new RestTemplate();

  @GetMapping("/oauth2callback")
  public ResponseEntity<?> handleGoogleCallback(@RequestParam("code") String code) {

    String tokenUrl = "https://oauth2.googleapis.com/token";

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("code", code);
    params.add("client_id", clientId);
    params.add("client_secret", clientSecret);
    params.add("redirect_uri", "https://eventmanagement-backend-ka9x.onrender.com/oauth2callback");
    params.add("grant_type", "authorization_code");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

    ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

    return ResponseEntity.ok(response.getBody());
  }
}
