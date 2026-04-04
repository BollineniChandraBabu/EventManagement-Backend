package com.familywishes.chat;

import static com.familywishes.chat.ChatDtos.*;

import com.familywishes.dto.CommonDtos.PagedResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ChatService {
  java.util.List<ChatUserResponse> listAvailableUsers();

  PagedResponse<ChatUserResponse> listAvailableUsers(
      int page, int size, String searchKey, String sortBy, String sortDir);

  java.util.List<ChatUserResponse> listActiveUsers();

  PagedResponse<ChatUserResponse> listActiveUsers(
      int page, int size, String searchKey, String sortBy, String sortDir);

  MessageResponse sendMessage(SendMessageRequest request, MultipartFile attachment);

  MessageResponse sendRealtimeTextMessage(Long senderId, Long receiverId, String messageText);

  MessagePageResponse getConversationMessages(Long otherUserId, int page, int size, boolean markSeen);

  java.util.List<ConversationResponse> listConversations();

  PagedResponse<ConversationResponse> listConversations(
      int page, int size, String searchKey, String sortBy, String sortDir);

  GlobalMessagePageResponse listAllMessages(int page, int size, String searchKey);

  void heartbeat();

  void markOffline();

  void heartbeat(Long userId);

  void markOffline(Long userId);

  void markConversationSeen(Long viewerId, Long otherUserId);

  byte[] downloadAttachment(Long messageId);

  DeleteMessageResponse deleteLastSentMessage(Long otherUserId);

  MessageResponse editMessage(Long messageId, EditMessageRequest request);

  MessageReactionsResponse listReactions(Long messageId);

  MessageReactionsResponse reactToMessage(Long messageId, MessageReactionRequest request);

  MessageReactionsResponse likeMessage(Long messageId, MessageReactionRequest request);

  MessageReactionsResponse unlikeMessage(Long messageId, MessageReactionRequest request);
}
