package com.familywishes.chat;

import com.familywishes.entity.User;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.GmailEmailService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnreadChatEmailScheduler {
  private final ChatMessageRepository chatMessageRepository;
  private final UserRepository userRepository;
  private final GmailEmailService gmailEmailService;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  @Scheduled(
      cron = "${app.chat.unread-mail.cron:0 0 2 * * *}",
      zone = "${scheduler.time-zone:Asia/Kolkata}")
  public void sendUnreadChatNotifications() {
    List<Map<String, Object>> rows = chatMessageRepository.findUnreadCountsByReceiver();

    for (Map<String, Object> row : rows) {
      Long receiverId = ((Number) row.get("receiver_id")).longValue();
      long unreadCount = ((Number) row.get("unread_count")).longValue();
      userRepository
          .findById(receiverId)
          .filter(u -> !u.isDeleted() && u.isActive())
          .ifPresent(user -> sendMail(user, unreadCount));
    }

    log.info("Unread chat mail scheduler executed in zone {}", schedulerTimeZone);
  }

  private void sendMail(User user, long unreadCount) {
    String subject = "You have unread chat messages";
    String html =
        "<h3>Hello "
            + user.getName()
            + ",</h3><p>You have <strong>"
            + unreadCount
            + " unread</strong> chat messages in Family Wishes.</p>"
            + "<p>Please open the app to respond.</p>";

    gmailEmailService.sendEmailWithAttachments(user.getEmail(), subject, html, null, null);
  }
}
