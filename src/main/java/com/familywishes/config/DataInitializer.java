package com.familywishes.config;

import com.familywishes.entity.EmailTemplate;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.Role;
import com.familywishes.repository.EmailTemplateRepository;
import com.familywishes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final EmailTemplateRepository templateRepository;

    @Override
    public void run(String... args) {
        userRepository.findByEmailAndDeletedFalse("chandrababubollineni416@gmail.com").orElseGet(() ->
                userRepository.save(User.builder().name("Default Admin").email("chandrababubollineni416@gmail.com")
                        .password(encoder.encode("Chandu")).role(Role.ROLE_ADMIN).active(true).deleted(false).build()));

        List<String> festivals = List.of("Diwali", "Pongal", "Sankranti", "Christmas", "New Year");
        for (String f : festivals) {
            if (templateRepository.findByNameOrderByVersionDesc(f).isEmpty()) {
                templateRepository.save(EmailTemplate.builder().name(f).subject("Happy " + f + " {{name}}")
                        .htmlContent("<h2>Happy " + f + " {{name}}!</h2><p>Wishing your family joy and prosperity.</p>")
                        .version(1).build());
            }
        }
    }
}
