package com.familywishes.service.impl;

import com.familywishes.dto.CircleDtos.*;
import com.familywishes.entity.Circle;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.CircleStatus;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.CircleRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.CircleService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CircleServiceImpl implements CircleService {

  private final CircleRepository circleRepository;
  private final UserRepository userRepository;
  private final SupabaseStorageService storageService;
  private final CircleRequestNotifier circleRequestNotifier;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  @Override
  @Transactional(readOnly = true)
  public CircleListResponse listCircles() {
    User me = currentUser();
    List<CircleResponse> circles = new ArrayList<>();
    for (Circle circle : circleRepository.findAllForUser(me.getId(), CircleStatus.ACCEPTED)) {
      User other = otherUser(circle, me.getId());
      circles.add(toCircleResponse(circle, other));
    }
    List<CircleRequestResponse> incoming = new ArrayList<>();
    for (Circle circle : circleRepository.findAllByAddressee(me.getId(), CircleStatus.PENDING)) {
      incoming.add(toRequestResponse(circle, circle.getRequester(), true));
    }
    List<CircleRequestResponse> outgoing = new ArrayList<>();
    for (Circle circle : circleRepository.findAllByRequester(me.getId(), CircleStatus.PENDING)) {
      outgoing.add(toRequestResponse(circle, circle.getAddressee(), false));
    }
    return new CircleListResponse(circles, incoming, outgoing);
  }

  @Override
  @Transactional
  public CircleRequestResponse sendRequest(SendCircleRequest request) {
    User me = currentUser();
    if (request.userId() == null) {
      throw new BadRequestException("User id is required");
    }
    if (me.getId().equals(request.userId())) {
      throw new BadRequestException("You cannot send a circle request to yourself");
    }
    User target =
        userRepository
            .findById(request.userId())
            .filter(u -> !u.isDeleted() && u.isActive())
            .orElseThrow(() -> new NotFoundException("User not found"));

    Circle existing = circleRepository.findBetween(me.getId(), target.getId()).orElse(null);
    if (existing != null) {
      if (existing.getStatus() == CircleStatus.ACCEPTED) {
        throw new BadRequestException("This user is already in your circle");
      }
      if (existing.getStatus() == CircleStatus.PENDING) {
        // The target had already asked us: sending back is an acceptance.
        if (existing.getAddressee().getId().equals(me.getId())) {
          return acceptExisting(existing, me);
        }
        throw new BadRequestException("A circle request is already pending");
      }
      // Previously rejected: reuse the row and flip the direction to the current requester.
      existing.setRequester(me);
      existing.setAddressee(target);
      existing.setStatus(CircleStatus.PENDING);
      existing.setRespondedAt(null);
      Circle saved = circleRepository.save(existing);
      notifyNewRequest(saved, me);
      return toRequestResponse(saved, target, false);
    }

    Circle circle =
        Circle.builder().requester(me).addressee(target).status(CircleStatus.PENDING).build();
    Circle saved = circleRepository.save(circle);
    notifyNewRequest(saved, me);
    return toRequestResponse(saved, target, false);
  }

  @Override
  @Transactional
  public CircleRequestResponse accept(CircleRequestAction request) {
    User me = currentUser();
    Circle circle = requirePendingForAddressee(request.circleId(), me.getId());
    return acceptExisting(circle, me);
  }

  @Override
  @Transactional
  public void reject(CircleRequestAction request) {
    User me = currentUser();
    Circle circle = requirePendingForAddressee(request.circleId(), me.getId());
    circle.setStatus(CircleStatus.REJECTED);
    circle.setRespondedAt(now());
    circleRepository.save(circle);
  }

  @Override
  @Transactional
  public void remove(CircleRequestAction request) {
    User me = currentUser();
    Circle circle =
        circleRepository
            .findById(request.circleId())
            .orElseThrow(() -> new NotFoundException("Circle not found"));
    if (!isParticipant(circle, me.getId())) {
      throw new NotFoundException("Circle not found");
    }
    circleRepository.delete(circle);
  }

  @Override
  @Transactional(readOnly = true)
  public CircleStatusResponse statusWith(Long userId) {
    User me = currentUser();
    if (userId == null) {
      throw new BadRequestException("User id is required");
    }
    Circle circle = circleRepository.findBetween(me.getId(), userId).orElse(null);
    if (circle == null) {
      return new CircleStatusResponse(userId, null, false, null);
    }
    boolean inCircle = circle.getStatus() == CircleStatus.ACCEPTED;
    return new CircleStatusResponse(userId, circle.getStatus(), inCircle, circle.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean areInCircle(Long userA, Long userB) {
    if (userA == null || userB == null || userA.equals(userB)) {
      return false;
    }
    return circleRepository.areInCircle(userA, userB);
  }

  @Override
  @Transactional
  public int sendPendingReminders(int days) {
    LocalDateTime cutoff = now().minusDays(days);
    LocalDate today = LocalDate.now(ZoneId.of(schedulerTimeZone));
    List<Circle> due = circleRepository.findPendingDueForReminder(cutoff, today);
    for (Circle circle : due) {
      circleRequestNotifier.notifyPendingReminder(circle);
      circle.setLastReminderSentOn(today);
    }
    if (!due.isEmpty()) {
      circleRepository.saveAll(due);
      log.info("Sent {} pending circle-request reminders (older than {} days)", due.size(), days);
    }
    return due.size();
  }

  private CircleRequestResponse acceptExisting(Circle circle, User me) {
    circle.setStatus(CircleStatus.ACCEPTED);
    circle.setRespondedAt(now());
    Circle saved = circleRepository.save(circle);
    notifyAccepted(saved);
    User other = otherUser(saved, me.getId());
    return toRequestResponse(saved, other, false);
  }

  /** Never lets a notification problem break the request itself. */
  private void notifyNewRequest(Circle circle, User requester) {
    try {
      circleRequestNotifier.notifyNewRequest(circle, requester);
    } catch (Exception e) {
      log.error("Circle request notification failed for circle {}", circle.getId(), e);
    }
  }

  /**
   * Tells the requester their request was accepted; a mail problem must not undo the acceptance.
   */
  private void notifyAccepted(Circle circle) {
    try {
      circleRequestNotifier.notifyRequestAccepted(circle);
    } catch (Exception e) {
      log.error("Circle accepted notification failed for circle {}", circle.getId(), e);
    }
  }

  private Circle requirePendingForAddressee(Long circleId, Long addresseeId) {
    if (circleId == null) {
      throw new BadRequestException("Circle id is required");
    }
    Circle circle =
        circleRepository
            .findById(circleId)
            .orElseThrow(() -> new NotFoundException("Circle request not found"));
    if (!circle.getAddressee().getId().equals(addresseeId)) {
      throw new BadRequestException("Only the addressee can respond to this circle request");
    }
    if (circle.getStatus() != CircleStatus.PENDING) {
      throw new BadRequestException("This circle request has already been handled");
    }
    return circle;
  }

  private boolean isParticipant(Circle circle, Long userId) {
    return circle.getRequester().getId().equals(userId)
        || circle.getAddressee().getId().equals(userId);
  }

  private User otherUser(Circle circle, Long me) {
    return circle.getRequester().getId().equals(me) ? circle.getAddressee() : circle.getRequester();
  }

  private CircleResponse toCircleResponse(Circle circle, User other) {
    return new CircleResponse(
        circle.getId(),
        other.getId(),
        other.getName(),
        other.getEmail(),
        resolveProfilePictureUrl(other),
        other.isOnline(),
        other.getLastSeenAt());
  }

  private CircleRequestResponse toRequestResponse(Circle circle, User other, boolean incoming) {
    return new CircleRequestResponse(
        circle.getId(),
        other.getId(),
        other.getName(),
        other.getEmail(),
        resolveProfilePictureUrl(other),
        incoming,
        circle.getStatus(),
        circle.getCreatedAt());
  }

  private String resolveProfilePictureUrl(User user) {
    if (user == null) {
      return null;
    }
    if (user.getProfilePictureUrl() == null || user.getProfilePictureUrl().isBlank()) {
      return storageService.getDefaultProfilePictureUrl(user.getGender());
    }
    return storageService.resolveProfilePictureUrl(user.getProfilePictureUrl());
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneId.of(schedulerTimeZone));
  }

  private User currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new NotFoundException("Authenticated user not found");
    }
    return userRepository
        .findByEmailAndDeletedFalse(authentication.getName())
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
