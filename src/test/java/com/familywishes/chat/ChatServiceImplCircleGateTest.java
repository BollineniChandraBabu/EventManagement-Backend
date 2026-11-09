package com.familywishes.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import com.familywishes.chat.ChatDtos.MessageResponse;
import com.familywishes.chat.ChatDtos.SendMessageRequest;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.Gender;
import com.familywishes.entity.enums.Role;
import com.familywishes.exception.BadRequestException;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.CircleService;
import com.familywishes.service.impl.SupabaseStorageService;
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

/**
 * Verifies the chat circle gate: when {@code app.chat.circles-only} is on, a message to someone
 * outside the sender's accepted circle is rejected, and when it is off any user may be messaged.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplCircleGateTest {

  @Mock private ChatMessageRepository chatMessageRepository;
  @Mock private UserRepository userRepository;
  @Mock private SupabaseStorageService storageService;
  @Mock private ChatRealtimePublisher realtimePublisher;
  @Mock private CircleService circleService;

  @InjectMocks private ChatServiceImpl chatService;

  private static final String ME_EMAIL = "me@example.com";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(chatService, "schedulerTimeZone", "Asia/Kolkata");
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(ME_EMAIL, "pwd", List.of()));
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

  private void authenticateCurrentUser() {
    when(userRepository.findByEmailAndDeletedFalse(ME_EMAIL))
        .thenReturn(Optional.of(user(1L, ME_EMAIL)));
    when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "other@example.com")));
  }

  @Test
  void rejectsMessageToUserOutsideCircleWhenRestricted() {
    ReflectionTestUtils.setField(chatService, "circlesOnlyChat", true);
    authenticateCurrentUser();
    when(circleService.areInCircle(1L, 2L)).thenReturn(false);

    SendMessageRequest request =
        new SendMessageRequest(2L, "hello", null, null, null, null, null, null);

    BadRequestException error =
        assertThrows(BadRequestException.class, () -> chatService.sendMessage(request, null));
    assertTrue(error.getMessage().toLowerCase().contains("circle"));

    verifyNoInteractions(chatMessageRepository);
    verify(realtimePublisher, never()).publishMessage(anyLong(), any());
  }

  @Test
  void allowsMessageToUserInCircleWhenRestricted() {
    ReflectionTestUtils.setField(chatService, "circlesOnlyChat", true);
    authenticateCurrentUser();
    when(circleService.areInCircle(1L, 2L)).thenReturn(true);
    when(chatMessageRepository.findConversationId(1L, 2L)).thenReturn(500L);
    when(chatMessageRepository.insertMessage(
            eq(500L),
            eq(1L),
            eq(2L),
            isNull(),
            eq("hello"),
            isNull(),
            isNull(),
            isNull(),
            anyString(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            any()))
        .thenReturn(9001L);

    SendMessageRequest request =
        new SendMessageRequest(2L, "hello", null, null, null, null, null, null);

    MessageResponse response = chatService.sendMessage(request, null);

    assertEquals(9001L, response.messageId());
    assertEquals(1L, response.senderId());
    assertEquals(2L, response.receiverId());
    verify(realtimePublisher).publishMessage(eq(500L), any(MessageResponse.class));
  }

  @Test
  void allowsMessageToAnyoneWhenRestrictionDisabled() {
    ReflectionTestUtils.setField(chatService, "circlesOnlyChat", false);
    authenticateCurrentUser();
    when(chatMessageRepository.findConversationId(1L, 2L)).thenReturn(500L);
    when(chatMessageRepository.insertMessage(
            eq(500L),
            eq(1L),
            eq(2L),
            isNull(),
            eq("hello"),
            isNull(),
            isNull(),
            isNull(),
            anyString(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            any()))
        .thenReturn(9001L);

    SendMessageRequest request =
        new SendMessageRequest(2L, "hello", null, null, null, null, null, null);

    MessageResponse response = chatService.sendMessage(request, null);

    assertEquals(9001L, response.messageId());
    verifyNoInteractions(circleService);
  }

  @Test
  void rejectsRealtimeMessageToUserOutsideCircleWhenRestricted() {
    ReflectionTestUtils.setField(chatService, "circlesOnlyChat", true);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "sender@example.com")));
    when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "other@example.com")));
    when(circleService.areInCircle(1L, 2L)).thenReturn(false);

    BadRequestException error =
        assertThrows(
            BadRequestException.class,
            () -> chatService.sendRealtimeMessage(1L, 2L, "hello", null, null, null, null, null));
    assertTrue(error.getMessage().toLowerCase().contains("circle"));

    verifyNoInteractions(chatMessageRepository);
  }
}
