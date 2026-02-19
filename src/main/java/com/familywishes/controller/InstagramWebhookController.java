package com.familywishes.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class InstagramWebhookController {

    private static final String VERIFY_TOKEN = "familywish_verify_token";

    @GetMapping
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

    @PostMapping
    public ResponseEntity<String> receiveWebhook(@RequestBody String payload) {

        System.out.println("Instagram Webhook Event:");
        System.out.println(payload);

        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}