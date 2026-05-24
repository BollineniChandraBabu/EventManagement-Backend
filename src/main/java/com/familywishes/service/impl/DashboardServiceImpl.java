package com.familywishes.service.impl;

import com.familywishes.dto.DashboardDtos.DashboardGraphPoint;
import com.familywishes.dto.DashboardDtos.DashboardGraphResponse;
import com.familywishes.dto.DashboardDtos.DashboardResponse;
import com.familywishes.dto.DashboardDtos.LoginLocationChartPoint;
import com.familywishes.dto.DashboardDtos.LoginLocationChartResponse;
import com.familywishes.entity.MessageStatus;
import com.familywishes.entity.User;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.entity.enums.EmailType;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.repository.EventRepository;
import com.familywishes.repository.IGMessageLogRepository;
import com.familywishes.repository.InstagramUserRepository;
import com.familywishes.repository.LoginLocationEventRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.service.DashboardService;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  private static final List<EmailType> SENSITIVE_TYPES =
      List.of(EmailType.OTP, EmailType.FORGOT_PASSWORD);

  private final UserRepository userRepository;
  private final EventRepository eventRepository;
  private final EmailLogRepository emailLogRepository;
  private final IGMessageLogRepository igMessageLogRepository;
  private final InstagramUserRepository instagramUserRepository;
  private final LoginLocationEventRepository loginLocationEventRepository;

  @Override
  public DashboardResponse getDashboard(String requesterEmail, boolean isAdmin) {
    LocalDate today = LocalDate.now(ZoneId.of(schedulerTimeZone));
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
            .countByRecipientUserEmailAndStatusAndSentAtGreaterThanEqualAndSentAtLessThanAndEmailTypeNotIn(
                requesterEmail, EmailStatus.SENT, startOfDay, startOfNextDay, SENSITIVE_TYPES);
    long failedEmails =
        emailLogRepository.countByRecipientUserEmailAndStatusAndEmailTypeNotIn(
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
              LocalDate.now(ZoneId.of(schedulerTimeZone)));

      LocalDate today = LocalDate.now(ZoneId.of(schedulerTimeZone));
      LocalDateTime startOfDay = today.atStartOfDay();
      LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();

      long messagesSentToday =
          igMessageLogRepository.countByStatusAndCreatedAtBetween(
              MessageStatus.SENT, startOfDay, startOfNextDay);
      long failedMessages = igMessageLogRepository.countByStatus(MessageStatus.FAILED);

      return new DashboardResponse(
          totalInstaUsers, upcomingEvents, messagesSentToday, failedMessages);
    }

    LocalDate today = LocalDate.now(ZoneId.of(schedulerTimeZone));
    User user = userRepository.findByEmailAndDeletedFalse(requesterEmail).orElse(null);
    long upcomingEvents =
        user == null
            ? 0L
            : eventRepository.countByUser_IdAndEventDateGreaterThanEqualAndActiveTrue(
                user.getId(), today);

    return new DashboardResponse(user == null ? 0L : 1L, upcomingEvents, 0L, 0L);
  }

  @Override
  public DashboardGraphResponse getMailChart(LocalDate startDate, LocalDate endDate, String requesterEmail, boolean isAdmin) {
    LocalDateTime start = startDate.atStartOfDay();
    int days = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;

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

    return buildGraphResponse(days, startDate, sentByDate, failedByDate);
  }

  @Override
  public DashboardGraphResponse getInstaChart(LocalDate startDate, LocalDate endDate, String requesterEmail, boolean isAdmin) {
    int days = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    if (!isAdmin) {
      return buildGraphResponse(days, startDate, Map.of(), Map.of());
    }

    LocalDateTime start = startDate.atStartOfDay();

    Map<LocalDate, Long> sentByDate =
        toDateCountMap(igMessageLogRepository.getDailyCountsByStatus(start, MessageStatus.SENT));
    Map<LocalDate, Long> failedByDate =
        toDateCountMap(igMessageLogRepository.getDailyCountsByStatus(start, MessageStatus.FAILED));

    return buildGraphResponse(days, startDate, sentByDate, failedByDate);
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
  public DashboardGraphResponse getOtpChart(LocalDate startDate, LocalDate endDate) {
    return getSensitiveChart(startDate, endDate, EmailType.OTP);
  }

  @Override
  public DashboardGraphResponse getForgotPasswordChart(LocalDate startDate, LocalDate endDate) {
    return getSensitiveChart(startDate, endDate, EmailType.FORGOT_PASSWORD);
  }

  private DashboardResponse getSensitiveDashboard(EmailType emailType) {
    LocalDate today = LocalDate.now(ZoneId.of(schedulerTimeZone));
    LocalDateTime startOfDay = today.atStartOfDay();
    LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();
    List<EmailType> types = List.of(emailType);

    long sentToday =
        emailLogRepository.countByStatusAndSentAtGreaterThanEqualAndSentAtLessThanAndEmailTypeIn(
            EmailStatus.SENT, startOfDay, startOfNextDay, types);
    long failed = emailLogRepository.countByStatusAndEmailTypeIn(EmailStatus.FAILED, types);

    return new DashboardResponse(0L, 0L, sentToday, failed);
  }

  private DashboardGraphResponse getSensitiveChart(LocalDate startDate, LocalDate endDate, EmailType emailType) {
    int days = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
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

    return buildGraphResponse(days, startDate, sentByDate, failedByDate);
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


  @Override
  public LoginLocationChartResponse getLoginLocationChart(
      Long userId, String fromDate, String toDate, String requesterEmail, boolean isAdmin) {
    LocalDateTime from =
        (fromDate == null || fromDate.isBlank()) ? null : LocalDate.parse(fromDate).atStartOfDay();
    String normalizedToDate =
        (toDate == null || toDate.isBlank())
            ? LocalDate.now(ZoneId.of(schedulerTimeZone)).toString()
            : toDate;
    LocalDateTime to =
        LocalDate.parse(normalizedToDate).plusDays(1).atStartOfDay().minusNanos(1);

    Long effectiveUserId = resolveEffectiveUserId(userId, requesterEmail, isAdmin);

    List<LoginLocationChartPoint> points =
        loginLocationEventRepository.countByLocationWithFilters(effectiveUserId, from, to).stream()
            .map(
                row ->
                    new LoginLocationChartPoint(
                        String.valueOf(row[0]),
                        row[1] == null ? null : String.valueOf(row[1]),
                        row[2] == null ? null : ((Number) row[2]).doubleValue(),
                        row[3] == null ? null : ((Number) row[3]).doubleValue(),
                        ((Number) row[4]).longValue()))
            .collect(Collectors.toList());

    return new LoginLocationChartResponse(effectiveUserId, fromDate, normalizedToDate, points);
  }


  private Long resolveEffectiveUserId(Long requestedUserId, String requesterEmail, boolean isAdmin) {
    if (isAdmin) {
      return requestedUserId;
    }

    return userRepository.findByEmailAndDeletedFalse(requesterEmail).map(User::getId).orElse(-1L);
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
