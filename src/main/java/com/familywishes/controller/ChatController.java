package com.familywishes.controller;

import com.familywishes.chat.ChatDtos;
import com.familywishes.chat.ChatService;
import com.familywishes.dto.CommonDtos.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

  private final ChatService chatService;

  @GetMapping("/users")
  public PagedResponse<ChatDtos.ChatUserResponse> users(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "name") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDir) {
    return chatService.listAvailableUsers(page, size, searchKey, sortBy, sortDir);
  }

  @GetMapping("/users/active")
  public PagedResponse<ChatDtos.ChatUserResponse> activeUsers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "") String searchKey,
      @RequestParam(defaultValue = "name") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDir) {
    return chatService.listActiveUsers(page, size, searchKey, sortBy, sortDir);
  }

  @GetMapping("/conversations")
  public PagedResponse<ChatDtos.ConversationResponse> conversations(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "") String searchKey) {
    return chatService.listConversations(page, size, searchKey, "lastMessageAt", "desc");
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ChatDtos.MessageResponse sendMessage(
      @RequestPart("payload") @Valid ChatDtos.SendMessageRequest payload,
      @RequestPart(value = "attachment", required = false) MultipartFile attachment) {
    return chatService.sendMessage(payload, attachment);
  }

  @GetMapping("/messages/{otherUserId}")
  public ChatDtos.MessagePageResponse conversationMessages(
      @PathVariable Long otherUserId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size,
      @RequestParam(defaultValue = "true") boolean markSeen) {
    return chatService.getConversationMessages(otherUserId, page, size, markSeen);
  }

  @GetMapping(
      value = "/messages/{messageId}/attachment",
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public byte[] downloadAttachment(@PathVariable Long messageId) {
    return chatService.downloadAttachment(messageId);
  }

  @DeleteMapping("/messages/last/{otherUserId}")
  public ChatDtos.DeleteMessageResponse deleteLastSentMessage(@PathVariable Long otherUserId) {
    return chatService.deleteLastSentMessage(otherUserId);
  }

  @PatchMapping("/messages/{messageId}")
  public ChatDtos.MessageResponse editMessage(
      @PathVariable Long messageId, @RequestBody @Valid ChatDtos.EditMessageRequest payload) {
    return chatService.editMessage(messageId, payload);
  }

  @PostMapping("/presence/heartbeat")
  public void heartbeat() {
    chatService.heartbeat();
  }

  @PostMapping("/presence/offline")
  public void offline() {
    chatService.markOffline();
  }

  @GetMapping("/messages/all")
  public ChatDtos.GlobalMessagePageResponse allMessages(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      @RequestParam(defaultValue = "") String searchKey) {
    return chatService.listAllMessages(page, size, searchKey);
  }
}
