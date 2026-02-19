package com.familywishes.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/instagram")
public class InstagramController {

    @Value("${instagram.verify.token}")
    private String VERIFY_TOKEN;

    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token) {

        System.out.println("INSTA token: " +token);
        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(403).body("Verification failed");
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> receiveWebhook(@RequestBody String payload) {

        System.out.println("Instagram Webhook Event:");
        System.out.println(payload);

        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    @GetMapping("/callback")
    public ResponseEntity<String> instagramCallback(
            @RequestParam("code") String code) {
        System.out.println("Authorization Code: " + code);
        return ResponseEntity.ok("Instagram login successful. Code: " + code);
    }
}