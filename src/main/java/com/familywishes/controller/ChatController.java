package com.familywishes.controller;

import com.familywishes.chat.ChatDtos;
import com.familywishes.chat.ChatService;
import jakarta.validation.Valid;
import java.util.List;
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
  public List<ChatDtos.ChatUserResponse> users() {
    return chatService.listAvailableUsers();
  }

  @GetMapping("/users/active")
  public List<ChatDtos.ChatUserResponse> activeUsers() {
    return chatService.listActiveUsers();
  }

  @GetMapping("/conversations")
  public List<ChatDtos.ConversationResponse> conversations() {
    return chatService.listConversations();
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
