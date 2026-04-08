package com.familywishes.chat;

import com.familywishes.entity.User;
import com.familywishes.entity.enums.EmailType;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.GmailEmailService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnreadChatEmailScheduler implements Job {
  private final ChatMessageRepository chatMessageRepository;
  private final UserRepository userRepository;
  private final GmailEmailService gmailEmailService;

  @Value("${app.password-reset.ui-url:http://localhost:4200/login}")
  private String loginUiUrl;

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  @Override
  public void execute(JobExecutionContext context) {
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
    String subject = "You have unread chat messages in Golden Greetings";
    String html =
        "<div style='font-family:Arial,sans-serif;line-height:1.5;color:#111827'>"
            + "<h3 style='margin-bottom:8px'>Hello "
            + user.getName()
            + ",</h3>"
            + "<p style='margin:0 0 12px 0'>You have <strong>"
            + unreadCount
            + " unread</strong> chat message(s) in Golden Greetings.</p>"
            + "<p style='margin:0 0 12px 0'>Please login to your account to continue the conversation.</p>"
            + "<p style='margin:0 0 12px 0'><a href='"
            + loginUiUrl
            + "' style='color:#2563eb;font-weight:600'>Login to Golden Greetings</a></p>"
            + "<p style='margin:0'>Thanks and regards,<br/>Golden Greetings Team</p>"
            + "</div>";

    gmailEmailService.sendEmailWithAttachments(
        user.getEmail(), subject, html, null, null, EmailType.UNREAD_CHAT_MESSAGE);
  }
}
