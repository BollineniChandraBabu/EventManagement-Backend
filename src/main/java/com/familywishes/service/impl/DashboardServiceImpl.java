package com.familywishes.service.impl;

import com.familywishes.dto.DashboardDtos.DashboardGraphPoint;
import com.familywishes.dto.DashboardDtos.DashboardGraphResponse;
import com.familywishes.dto.DashboardDtos.DashboardResponse;
import com.familywishes.entity.MessageStatus;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.entity.enums.EmailType;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.repository.EventRepository;
import com.familywishes.repository.IGMessageLogRepository;
import com.familywishes.repository.InstagramUserRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.DashboardService;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
  private static final List<EmailType> SENSITIVE_TYPES =
      List.of(EmailType.OTP, EmailType.FORGOT_PASSWORD);

  private final UserRepository userRepository;
  private final EventRepository eventRepository;
  private final EmailLogRepository emailLogRepository;
  private final IGMessageLogRepository igMessageLogRepository;
  private final InstagramUserRepository instagramUserRepository;

  @Override
  public DashboardResponse getDashboard(String requesterEmail, boolean isAdmin) {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();

    if (isAdmin) {
      long totalUsers = userRepository.countByDeletedFalse();
      long upcomingEvents = eventRepository.countByEventDateGreaterThanEqualAndActiveTrue(today);

      long emailsSentToday =
          emailLogRepository
              .countByStatusAndSentAtGreaterThanEqualAndSentAtLessThanAndEmailTypeNotIn(
                  EmailStatus.SENT, startOfDay, startOfNextDay, SENSITIVE_TYPES);
      long failedEmails =
          emailLogRepository.countByStatusAndEmailTypeNotIn(EmailStatus.FAILED, SENSITIVE_TYPES);

      return new DashboardResponse(totalUsers, upcomingEvents, emailsSentToday, failedEmails);
    }

    User user = userRepository.findByEmailAndDeletedFalse(requesterEmail).orElse(null);
    long userCount = user == null ? 0L : 1L;
    long upcomingEvents =
        user == null
            ? 0L
            : eventRepository.countByUser_IdAndEventDateGreaterThanEqualAndActiveTrue(
                user.getId(), today);

    long emailsSentToday =
        emailLogRepository
            .countByRecipientEmailAndStatusAndSentAtGreaterThanEqualAndSentAtLessThanAndEmailTypeNotIn(
                requesterEmail, EmailStatus.SENT, startOfDay, startOfNextDay, SENSITIVE_TYPES);
    long failedEmails =
        emailLogRepository.countByRecipientEmailAndStatusAndEmailTypeNotIn(
            requesterEmail, EmailStatus.FAILED, SENSITIVE_TYPES);

    return new DashboardResponse(userCount, upcomingEvents, emailsSentToday, failedEmails);
  }

  @Override
  public DashboardResponse getIGDashboard(String requesterEmail, boolean isAdmin) {
    if (isAdmin) {
      long totalInstaUsers =
          instagramUserRepository.countByInstagramUserIdIsNotNullAndInstagramUserIdNot("");
      long upcomingEvents =
          eventRepository.countByEventDateGreaterThanEqualAndActiveTrue(
              LocalDate.now(ZoneId.of("Asia/Kolkata")));

      LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
      LocalDateTime startOfDay = today.atStartOfDay();
      LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();

      long messagesSentToday =
          igMessageLogRepository.countByStatusAndCreatedAtBetween(
              MessageStatus.SENT, startOfDay, startOfNextDay);
      long failedMessages = igMessageLogRepository.countByStatus(MessageStatus.FAILED);

      return new DashboardResponse(
          totalInstaUsers, upcomingEvents, messagesSentToday, failedMessages);
    }

    LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    User user = userRepository.findByEmailAndDeletedFalse(requesterEmail).orElse(null);
    long upcomingEvents =
        user == null
            ? 0L
            : eventRepository.countByUser_IdAndEventDateGreaterThanEqualAndActiveTrue(
                user.getId(), today);

    return new DashboardResponse(user == null ? 0L : 1L, upcomingEvents, 0L, 0L);
  }

  @Override
  public DashboardGraphResponse getMailChart(int days, String requesterEmail, boolean isAdmin) {
    int normalizedDays = Math.max(1, days);
    LocalDate startDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(normalizedDays - 1L);
    LocalDateTime start = startDate.atStartOfDay();

    Map<LocalDate, Long> sentByDate;
    Map<LocalDate, Long> failedByDate;

    if (isAdmin) {
      sentByDate =
          toDateCountMap(
              emailLogRepository.getDailyCountsByStatusAndEmailTypeNotIn(
                  start, EmailStatus.SENT, SENSITIVE_TYPES));
      failedByDate =
          toDateCountMap(
              emailLogRepository.getDailyCountsByStatusAndEmailTypeNotIn(
                  start, EmailStatus.FAILED, SENSITIVE_TYPES));
    } else {
      sentByDate =
          toDateCountMap(
              emailLogRepository.getDailyCountsByRecipientAndStatusAndEmailTypeNotIn(
                  start, EmailStatus.SENT, requesterEmail, SENSITIVE_TYPES));
      failedByDate =
          toDateCountMap(
              emailLogRepository.getDailyCountsByRecipientAndStatusAndEmailTypeNotIn(
                  start, EmailStatus.FAILED, requesterEmail, SENSITIVE_TYPES));
    }

    return buildGraphResponse(normalizedDays, startDate, sentByDate, failedByDate);
  }

  @Override
  public DashboardGraphResponse getInstaChart(int days, String requesterEmail, boolean isAdmin) {
    if (!isAdmin) {
      int normalizedDays = Math.max(1, days);
      LocalDate startDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(normalizedDays - 1L);
      return buildGraphResponse(normalizedDays, startDate, Map.of(), Map.of());
    }

    int normalizedDays = Math.max(1, days);
    LocalDate startDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(normalizedDays - 1L);
    LocalDateTime start = startDate.atStartOfDay();

    Map<LocalDate, Long> sentByDate =
        toDateCountMap(igMessageLogRepository.getDailyCountsByStatus(start, MessageStatus.SENT));
    Map<LocalDate, Long> failedByDate =
        toDateCountMap(igMessageLogRepository.getDailyCountsByStatus(start, MessageStatus.FAILED));

    return buildGraphResponse(normalizedDays, startDate, sentByDate, failedByDate);
  }

  @Override
  public DashboardResponse getOtpDashboard() {
    return getSensitiveDashboard(EmailType.OTP);
  }

  @Override
  public DashboardResponse getForgotPasswordDashboard() {
    return getSensitiveDashboard(EmailType.FORGOT_PASSWORD);
  }

  @Override
  public DashboardGraphResponse getOtpChart(int days) {
    return getSensitiveChart(days, EmailType.OTP);
  }

  @Override
  public DashboardGraphResponse getForgotPasswordChart(int days) {
    return getSensitiveChart(days, EmailType.FORGOT_PASSWORD);
  }

  private DashboardResponse getSensitiveDashboard(EmailType emailType) {
    LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();
    List<EmailType> types = List.of(emailType);

    long sentToday =
        emailLogRepository.countByStatusAndSentAtGreaterThanEqualAndSentAtLessThanAndEmailTypeIn(
            EmailStatus.SENT, startOfDay, startOfNextDay, types);
    long failed = emailLogRepository.countByStatusAndEmailTypeIn(EmailStatus.FAILED, types);

    return new DashboardResponse(0L, 0L, sentToday, failed);
  }

  private DashboardGraphResponse getSensitiveChart(int days, EmailType emailType) {
    int normalizedDays = Math.max(1, days);
    LocalDate startDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(normalizedDays - 1L);
    LocalDateTime start = startDate.atStartOfDay();
    List<EmailType> types = List.of(emailType);

    Map<LocalDate, Long> sentByDate =
        toDateCountMap(
            emailLogRepository.getDailyCountsByStatusAndEmailTypeIn(
                start, EmailStatus.SENT, types));
    Map<LocalDate, Long> failedByDate =
        toDateCountMap(
            emailLogRepository.getDailyCountsByStatusAndEmailTypeIn(
                start, EmailStatus.FAILED, types));

    return buildGraphResponse(normalizedDays, startDate, sentByDate, failedByDate);
  }

  private DashboardGraphResponse buildGraphResponse(
      int days,
      LocalDate startDate,
      Map<LocalDate, Long> sentByDate,
      Map<LocalDate, Long> failedByDate) {
    List<DashboardGraphPoint> points =
        java.util.stream.IntStream.range(0, days)
            .mapToObj(startDate::plusDays)
            .map(
                date -> {
                  long sent = sentByDate.getOrDefault(date, 0L);
                  long failed = failedByDate.getOrDefault(date, 0L);
                  return new DashboardGraphPoint(date.toString(), sent, failed, sent + failed);
                })
            .toList();

    return new DashboardGraphResponse(days, points);
  }

  private Map<LocalDate, Long> toDateCountMap(List<Object[]> rows) {
    Map<LocalDate, Long> result = new HashMap<>();
    for (Object[] row : rows) {
      LocalDate date = convertToLocalDate(row[0]);
      long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
      result.put(date, count);
    }
    return result;
  }

  private LocalDate convertToLocalDate(Object dateValue) {
    if (dateValue instanceof LocalDate localDate) {
      return localDate;
    }
    if (dateValue instanceof Date date) {
      return date.toLocalDate();
    }
    return LocalDate.parse(String.valueOf(dateValue));
  }
}
