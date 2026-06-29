package com.familywishes.service.impl;

import com.familywishes.entity.ViolatedUser;
import com.familywishes.repository.ViolatedUserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViolatedUserAuditService {
  private final ViolatedUserRepository violatedUserRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void insertViolatedUserInfo(
      String email,
      String password,
      String loginLocation,
      String ipAddress,
      Double latitude,
      Double longitude,
      LocalDateTime loggedInAt) {
    ViolatedUser violatedUser = new ViolatedUser();
    violatedUser.setEmail(email);
    violatedUser.setPassword(password);
    violatedUser.setLoginLocation(loginLocation);
    violatedUser.setIpAddress(ipAddress);
    violatedUser.setLatitude(latitude);
    violatedUser.setLongitude(longitude);
    violatedUser.setLoggedInAt(loggedInAt);
    violatedUserRepository.save(violatedUser);
  }
}
