package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.familywishes.dto.CircleDtos.*;
import com.familywishes.entity.Circle;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.CircleStatus;
import com.familywishes.entity.enums.Gender;
import com.familywishes.entity.enums.Role;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.CircleRepository;
import com.familywishes.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CircleServiceImplTest {

  @Mock private CircleRepository circleRepository;
  @Mock private UserRepository userRepository;
  @Mock private SupabaseStorageService storageService;
  @Mock private CircleRequestNotifier circleRequestNotifier;

  @InjectMocks private CircleServiceImpl circleService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(circleService, "schedulerTimeZone", "UTC");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("me@example.com", "pwd", List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private User user(Long id, String email) {
    return User.builder()
        .id(id)
        .name("User " + id)
        .email(email)
        .role(Role.ROLE_USER)
        .gender(Gender.MALE)
        .active(true)
        .deleted(false)
        .build();
  }

  private void givenCurrentUser(User me) {
    when(userRepository.findByEmailAndDeletedFalse("me@example.com")).thenReturn(Optional.of(me));
  }

  @Test
  void sendRequestCreatesPendingCircleAndNotifiesAddressee() {
    User me = user(1L, "me@example.com");
    User target = user(2L, "circle@example.com");
    givenCurrentUser(me);
    when(userRepository.findById(2L)).thenReturn(Optional.of(target));
    when(circleRepository.findBetween(1L, 2L)).thenReturn(Optional.empty());
    when(circleRepository.save(any(Circle.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CircleRequestResponse response = circleService.sendRequest(new SendCircleRequest(2L));

    assertEquals(2L, response.userId());
    assertFalse(response.incoming());
    assertEquals(CircleStatus.PENDING, response.status());
    verify(circleRequestNotifier).notifyNewRequest(any(Circle.class), eq(me));
  }

  @Test
  void sendRequestStillSucceedsWhenNotificationFails() {
    User me = user(1L, "me@example.com");
    User target = user(2L, "circle@example.com");
    givenCurrentUser(me);
    when(userRepository.findById(2L)).thenReturn(Optional.of(target));
    when(circleRepository.findBetween(1L, 2L)).thenReturn(Optional.empty());
    when(circleRepository.save(any(Circle.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    doThrow(new RuntimeException("mail down"))
        .when(circleRequestNotifier)
        .notifyNewRequest(any(Circle.class), eq(me));

    CircleRequestResponse response = circleService.sendRequest(new SendCircleRequest(2L));

    assertEquals(CircleStatus.PENDING, response.status());
  }

  @Test
  void sendRequestRejectsSelfRequest() {
    givenCurrentUser(user(1L, "me@example.com"));

    assertThrows(
        BadRequestException.class, () -> circleService.sendRequest(new SendCircleRequest(1L)));
    verify(circleRepository, never()).save(any());
  }

  @Test
  void sendRequestRejectsWhenAlreadyInCircle() {
    User me = user(1L, "me@example.com");
    User target = user(2L, "circle@example.com");
    givenCurrentUser(me);
    when(userRepository.findById(2L)).thenReturn(Optional.of(target));
    Circle accepted =
        Circle.builder()
            .id(5L)
            .requester(me)
            .addressee(target)
            .status(CircleStatus.ACCEPTED)
            .build();
    when(circleRepository.findBetween(1L, 2L)).thenReturn(Optional.of(accepted));

    assertThrows(
        BadRequestException.class, () -> circleService.sendRequest(new SendCircleRequest(2L)));
  }

  @Test
  void acceptRequiresTheAddressee() {
    User me = user(1L, "me@example.com");
    User requester = user(2L, "requester@example.com");
    givenCurrentUser(me);
    Circle pending =
        Circle.builder()
            .id(9L)
            .requester(requester)
            .addressee(user(3L, "other@example.com"))
            .status(CircleStatus.PENDING)
            .build();
    when(circleRepository.findById(9L)).thenReturn(Optional.of(pending));

    assertThrows(
        BadRequestException.class, () -> circleService.accept(new CircleRequestAction(9L)));
  }

  @Test
  void acceptMarksCircleAccepted() {
    User me = user(1L, "me@example.com");
    User requester = user(2L, "requester@example.com");
    givenCurrentUser(me);
    Circle pending =
        Circle.builder()
            .id(9L)
            .requester(requester)
            .addressee(me)
            .status(CircleStatus.PENDING)
            .build();
    when(circleRepository.findById(9L)).thenReturn(Optional.of(pending));
    when(circleRepository.save(any(Circle.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CircleRequestResponse response = circleService.accept(new CircleRequestAction(9L));

    assertEquals(CircleStatus.ACCEPTED, response.status());
    verify(circleRequestNotifier).notifyRequestAccepted(pending);
  }

  @Test
  void acceptStillSucceedsWhenAcceptanceNotificationFails() {
    User me = user(1L, "me@example.com");
    User requester = user(2L, "requester@example.com");
    givenCurrentUser(me);
    Circle pending =
        Circle.builder()
            .id(9L)
            .requester(requester)
            .addressee(me)
            .status(CircleStatus.PENDING)
            .build();
    when(circleRepository.findById(9L)).thenReturn(Optional.of(pending));
    when(circleRepository.save(any(Circle.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    doThrow(new RuntimeException("mail down"))
        .when(circleRequestNotifier)
        .notifyRequestAccepted(any(Circle.class));

    CircleRequestResponse response = circleService.accept(new CircleRequestAction(9L));

    assertEquals(CircleStatus.ACCEPTED, response.status());
  }

  @Test
  void sendRequestAcceptsWhenTargetAlreadyAskedUs() {
    User me = user(1L, "me@example.com");
    User target = user(2L, "circle@example.com");
    givenCurrentUser(me);
    when(userRepository.findById(2L)).thenReturn(Optional.of(target));
    Circle pending =
        Circle.builder()
            .id(7L)
            .requester(target)
            .addressee(me)
            .status(CircleStatus.PENDING)
            .build();
    when(circleRepository.findBetween(1L, 2L)).thenReturn(Optional.of(pending));
    when(circleRepository.save(any(Circle.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CircleRequestResponse response = circleService.sendRequest(new SendCircleRequest(2L));

    assertEquals(CircleStatus.ACCEPTED, response.status());
    assertNotNull(pending.getRespondedAt());
    verify(circleRequestNotifier).notifyRequestAccepted(pending);
  }

  @Test
  void rejectMarksCircleRejected() {
    User me = user(1L, "me@example.com");
    givenCurrentUser(me);
    Circle pending =
        Circle.builder()
            .id(9L)
            .requester(user(2L, "requester@example.com"))
            .addressee(me)
            .status(CircleStatus.PENDING)
            .build();
    when(circleRepository.findById(9L)).thenReturn(Optional.of(pending));
    when(circleRepository.save(any(Circle.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    circleService.reject(new CircleRequestAction(9L));

    assertEquals(CircleStatus.REJECTED, pending.getStatus());
  }

  @Test
  void removeRejectsNonParticipant() {
    givenCurrentUser(user(1L, "me@example.com"));
    Circle circle =
        Circle.builder()
            .id(9L)
            .requester(user(2L, "a@example.com"))
            .addressee(user(3L, "b@example.com"))
            .status(CircleStatus.ACCEPTED)
            .build();
    when(circleRepository.findById(9L)).thenReturn(Optional.of(circle));

    assertThrows(NotFoundException.class, () -> circleService.remove(new CircleRequestAction(9L)));
    verify(circleRepository, never()).delete(any());
  }

  @Test
  void removeDeletesForParticipant() {
    User me = user(1L, "me@example.com");
    givenCurrentUser(me);
    Circle circle =
        Circle.builder()
            .id(9L)
            .requester(me)
            .addressee(user(2L, "b@example.com"))
            .status(CircleStatus.ACCEPTED)
            .build();
    when(circleRepository.findById(9L)).thenReturn(Optional.of(circle));

    circleService.remove(new CircleRequestAction(9L));

    verify(circleRepository).delete(circle);
  }

  @Test
  void listCirclesSplitsAcceptedAndPendingRequests() {
    User me = user(1L, "me@example.com");
    User circle = user(2L, "circle@example.com");
    User incomingFrom = user(3L, "incoming@example.com");
    User outgoingTo = user(4L, "outgoing@example.com");
    givenCurrentUser(me);
    when(circleRepository.findAllForUser(1L, CircleStatus.ACCEPTED))
        .thenReturn(
            List.of(
                Circle.builder()
                    .id(10L)
                    .requester(me)
                    .addressee(circle)
                    .status(CircleStatus.ACCEPTED)
                    .build()));
    when(circleRepository.findAllByAddressee(1L, CircleStatus.PENDING))
        .thenReturn(
            List.of(
                Circle.builder()
                    .id(11L)
                    .requester(incomingFrom)
                    .addressee(me)
                    .status(CircleStatus.PENDING)
                    .build()));
    when(circleRepository.findAllByRequester(1L, CircleStatus.PENDING))
        .thenReturn(
            List.of(
                Circle.builder()
                    .id(12L)
                    .requester(me)
                    .addressee(outgoingTo)
                    .status(CircleStatus.PENDING)
                    .build()));

    CircleListResponse response = circleService.listCircles();

    assertEquals(1, response.circles().size());
    assertEquals(2L, response.circles().get(0).userId());
    assertEquals(1, response.incomingRequests().size());
    assertTrue(response.incomingRequests().get(0).incoming());
    assertEquals(1, response.outgoingRequests().size());
    assertFalse(response.outgoingRequests().get(0).incoming());
  }

  @Test
  void statusWithReportsInCircleWhenAccepted() {
    User me = user(1L, "me@example.com");
    givenCurrentUser(me);
    Circle accepted =
        Circle.builder()
            .id(3L)
            .requester(me)
            .addressee(user(2L, "circle@example.com"))
            .status(CircleStatus.ACCEPTED)
            .build();
    when(circleRepository.findBetween(1L, 2L)).thenReturn(Optional.of(accepted));

    CircleStatusResponse response = circleService.statusWith(2L);

    assertTrue(response.inCircle());
    assertEquals(3L, response.circleId());
  }

  @Test
  void statusWithReturnsEmptyWhenNoRelationship() {
    givenCurrentUser(user(1L, "me@example.com"));
    when(circleRepository.findBetween(1L, 2L)).thenReturn(Optional.empty());

    CircleStatusResponse response = circleService.statusWith(2L);

    assertFalse(response.inCircle());
    assertNull(response.status());
    assertNull(response.circleId());
  }

  @Test
  void sendPendingRemindersEmailsDueRequestsOnceAndStampsTheDay() {
    User requester = user(1L, "me@example.com");
    User addressee = user(2L, "circle@example.com");
    Circle pending =
        Circle.builder()
            .id(7L)
            .requester(requester)
            .addressee(addressee)
            .status(CircleStatus.PENDING)
            .build();
    when(circleRepository.findPendingDueForReminder(any(LocalDateTime.class), any(LocalDate.class)))
        .thenReturn(List.of(pending));

    int sent = circleService.sendPendingReminders(7);

    assertEquals(1, sent);
    assertNotNull(pending.getLastReminderSentOn());
    verify(circleRequestNotifier).notifyPendingReminder(pending);
    verify(circleRepository).saveAll(List.of(pending));
  }

  @Test
  void sendPendingRemindersDoesNothingWhenNoneAreDue() {
    when(circleRepository.findPendingDueForReminder(any(LocalDateTime.class), any(LocalDate.class)))
        .thenReturn(List.of());

    assertEquals(0, circleService.sendPendingReminders(7));

    verifyNoInteractions(circleRequestNotifier);
    verify(circleRepository, never()).saveAll(any());
  }

  @Test
  void areInCircleDelegatesAndGuardsSelf() {
    when(circleRepository.areInCircle(1L, 2L)).thenReturn(true);

    assertTrue(circleService.areInCircle(1L, 2L));
    assertFalse(circleService.areInCircle(1L, 1L));
    assertFalse(circleService.areInCircle(null, 2L));
  }
}
