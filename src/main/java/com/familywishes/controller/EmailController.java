package com.familywishes.controller;

import com.familywishes.dto.EmailDtos.EmailStatusResponse;
import com.familywishes.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/email", "/api/emails"})
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;

    @PostMapping("/test")
    public void test(Authentication authentication) { emailService.sendTestEmail(authentication.getName()); }

    @GetMapping("/status")
    public List<EmailStatusResponse> status() { return emailService.getStatus(); }
}
