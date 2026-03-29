package com.familywishes.chat;

import static com.familywishes.chat.ChatDtos.*;

import org.springframework.web.multipart.MultipartFile;

public interface ChatService {
  java.util.List<ChatUserResponse> listAvailableUsers();

  java.util.List<ChatUserResponse> listActiveUsers();

  MessageResponse sendMessage(SendMessageRequest request, MultipartFile attachment);

  MessageResponse sendRealtimeTextMessage(Long senderId, Long receiverId, String messageText);

  MessagePageResponse getConversationMessages(Long otherUserId, int page, int size, boolean markSeen);

  java.util.List<ConversationResponse> listConversations();

  GlobalMessagePageResponse listAllMessages(int page, int size, String searchKey);

  void heartbeat();

  void markOffline();

  void heartbeat(Long userId);

  void markOffline(Long userId);

  void markConversationSeen(Long viewerId, Long otherUserId);

  byte[] downloadAttachment(Long messageId);

  DeleteMessageResponse deleteLastSentMessage(Long otherUserId);
}
