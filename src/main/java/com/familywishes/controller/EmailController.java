package com.familywishes.controller;

import com.familywishes.dto.EmailDtos.EmailStatusResponse;
import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.service.GmailEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/email", "/api/emails"})
@RequiredArgsConstructor
public class EmailController {
    private final GmailEmailService emailService;

    @PostMapping("/test")
    public void test(Authentication authentication) { emailService.sendTestEmail(authentication.getName()); }

    @GetMapping("/status")
    public PagedResponse<EmailStatusResponse> status(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String searchKey
    ) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        return emailService.getStatus(page, size, searchKey, authentication.getName(), isAdmin);
    }
}
