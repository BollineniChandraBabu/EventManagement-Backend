package com.familywishes.config;

import com.familywishes.entity.User;
import com.familywishes.entity.enums.RelationShip;
import com.familywishes.entity.enums.Role;
import com.familywishes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
  private final UserRepository userRepository;
  private final PasswordEncoder encoder;

  @Override
  public void run(String... args) {
    userRepository
        .findByEmailAndDeletedFalse("chandrababubollineni416@gmail.com")
        .orElseGet(
            () ->
                userRepository.save(
                    User.builder()
                        .name("Default Admin")
                        .email("chandrababubollineni416@gmail.com")
                        .password(encoder.encode("Chandu"))
                        .role(Role.ROLE_ADMIN)
                        .active(true)
                        .deleted(false)
                        .relationShip(RelationShip.ADMIN)
                        .build()));
  }
}
