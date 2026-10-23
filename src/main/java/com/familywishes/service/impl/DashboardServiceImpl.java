package com.familywishes.service.impl;

import com.familywishes.config.CacheNames;
import com.familywishes.dto.DashboardDtos.DashboardGraphPoint;
import com.familywishes.dto.DashboardDtos.DashboardGraphResponse;
import com.familywishes.dto.DashboardDtos.DashboardResponse;
import com.familywishes.dto.DashboardDtos.LoginLocationChartPoint;
import com.familywishes.dto.DashboardDtos.LoginLocationChartResponse;
import com.familywishes.dto.DashboardDtos.ViolatedUserChartPoint;
import com.familywishes.dto.DashboardDtos.ViolatedUserEmailSummary;
import com.familywishes.dto.DashboardDtos.ViolatedUserInfo;
import com.familywishes.dto.DashboardDtos.ViolatedUserIpSummary;
import com.familywishes.dto.DashboardDtos.ViolatedUserIpInfo;
import com.familywishes.dto.DashboardDtos.ViolatedUserMapPoint;
import com.familywishes.dto.DashboardDtos.ViolatedUsersDashboardResponse;
import com.familywishes.entity.LoginLocationEvent;
import com.familywishes.entity.MessageStatus;
import com.familywishes.entity.User;
import com.familywishes.entity.ViolatedUser;
import com.familywishes.entity.enums.EmailStatus;
import com.familywishes.entity.enums.EmailType;
import com.familywishes.repository.EmailLogRepository;
import com.familywishes.repository.EventRepository;
import com.familywishes.repository.IGMessageLogRepository;
import com.familywishes.repository.InstagramUserRepository;
import com.familywishes.repository.LoginLocationEventRepository;
import com.familywishes.repository.UserRepository;
import com.familywishes.repository.ViolatedUserRepository;
import com.familywishes.service.DashboardService;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

  @Value("${scheduler.time-zone:Asia/Kolkata}")
  private String schedulerTimeZone;

  /** Most failed-login points drawn on the map at once. */
  private static final int MAX_VIOLATED_MAP_POINTS = 300;

  private static final int TOP_VIOLATION_LIMIT = 5;

  /** Most login events returned at once; the dashboard asks for a narrower range to see more. */
  private static final int MAX_LOGIN_LOCATION_POINTS = 1000;

  private static final List<EmailType> SENSITIVE_TYPES =
      List.of(EmailType.OTP, EmailType.FORGOT_PASSWORD);

  private final UserRepository userRepository;
  private final EventRepository eventRepository;
  private final EmailLogRepository emailLogRepository;
  private final IGMessageLogRepository igMessageLogRepository;
  private final InstagramUserRepository instagramUserRepository;
  private final LoginLocationEventRepository loginLocationEventRepository;
  private final ViolatedUserRepository violatedUserRepository;
  private final IpInfoService ipInfoService;

  @Override
  @Cacheable(
      cacheNames = CacheNames.DASHBOARD_SUMMARY,
      key = "#isAdmin ? 'admin' : #requesterEmail")
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
  public DashboardGraphResponse getMailChart(
      LocalDate startDate, LocalDate endDate, String requesterEmail, boolean isAdmin) {
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
  public DashboardGraphResponse getInstaChart(
      LocalDate startDate, LocalDate endDate, String requesterEmail, boolean isAdmin) {
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

  private DashboardGraphResponse getSensitiveChart(
      LocalDate startDate, LocalDate endDate, EmailType emailType) {
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
      Long userId,
      String fromDate,
      String toDate,
      String requesterEmail,
      boolean isAdmin,
      List<Long> userIds) {
    LocalDateTime from =
        (fromDate == null || fromDate.isBlank()) ? null : LocalDate.parse(fromDate).atStartOfDay();
    String normalizedToDate =
        (toDate == null || toDate.isBlank())
            ? LocalDate.now(ZoneId.of(schedulerTimeZone)).toString()
            : toDate;
    LocalDateTime to =
        LocalDate.parse(normalizedToDate).plusDays(1).atStartOfDay().minusNanos(1);

    Long effectiveUserId = resolveEffectiveUserId(userId, requesterEmail, isAdmin);
    if (Objects.nonNull(effectiveUserId)) {
      userIds = CollectionUtils.isEmpty(userIds) ? new ArrayList<>() : userIds;
      userIds.add(effectiveUserId);
    }
    // One extra row tells us whether older logins were left out.
    List<LoginLocationEvent> events =
        loginLocationEventRepository.findNewestWithUser(
            userIds, from, to, PageRequest.of(0, MAX_LOGIN_LOCATION_POINTS + 1));
    boolean truncated = events.size() > MAX_LOGIN_LOCATION_POINTS;

    List<LoginLocationChartPoint> points =
        events.stream().limit(MAX_LOGIN_LOCATION_POINTS).map(this::toLoginLocationPoint).toList();

    return new LoginLocationChartResponse(
        effectiveUserId, fromDate, normalizedToDate, points, truncated);
  }

  private LoginLocationChartPoint toLoginLocationPoint(LoginLocationEvent event) {
    return new LoginLocationChartPoint(
        event.getUser().getEmail(),
        event.getUser().getName(),
        event.getLoginLocation(),
        event.getIpAddress(),
        event.getLatitude(),
        event.getLongitude(),
        event.getLoggedInAt());
  }

  @Override
  public ViolatedUsersDashboardResponse getViolatedUsersDashboard(
      LocalDate startDate,
      LocalDate endDate,
      int page,
      int size,
      boolean includeIpInfo,
      String searchKey) {
    LocalDateTime start = startDate.atStartOfDay();
    LocalDateTime end = endDate.plusDays(1).atStartOfDay().minusNanos(1);
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "loggedInAt"));

    Page<ViolatedUser> violatedUsers =
        violatedUserRepository.search(
            start, end, searchKey == null ? "" : searchKey.trim(), pageable);
    // Only a bounded set of recent attempts is drawn on the map, and without IP lookups: looking
    // up every point would make one ip-api.com call each (the service allows 45 per minute).
    List<ViolatedUser> mapUsers =
        violatedUserRepository
            .findByLoggedInAtGreaterThanEqualAndLoggedInAtLessThanEqualAndLatitudeIsNotNullAndLongitudeIsNotNull(
                start,
                end,
                PageRequest.of(0, MAX_VIOLATED_MAP_POINTS, Sort.by(Sort.Direction.DESC, "loggedInAt")));
    long totalAttempts =
        violatedUserRepository.countByLoggedInAtGreaterThanEqualAndLoggedInAtLessThanEqual(
            start, end);

    Map<LocalDate, Long> attemptsByDate =
        toDateCountMap(violatedUserRepository.getDailyCountsByLoggedInAtBetween(start, end));
    int days = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    List<ViolatedUserChartPoint> chartPoints =
        java.util.stream.IntStream.range(0, days)
            .mapToObj(startDate::plusDays)
            .map(
                date ->
                    new ViolatedUserChartPoint(
                        date.toString(), attemptsByDate.getOrDefault(date, 0L)))
            .toList();

    List<ViolatedUserMapPoint> mapPoints =
        mapUsers.stream()
            .map(user -> toViolatedUserMapPoint(user, false))
            .toList();
    List<ViolatedUserInfo> users =
        violatedUsers.getContent().stream()
            .map(user -> toViolatedUserInfo(user, includeIpInfo))
            .toList();

    return new ViolatedUsersDashboardResponse(
        startDate.toString(),
        endDate.toString(),
        totalAttempts,
        chartPoints,
        mapPoints,
        users,
        violatedUsers.getNumber(),
        violatedUsers.getSize(),
        violatedUsers.getTotalElements(),
        violatedUsers.getTotalPages(),
        violatedUsers.hasNext(),
        violatedUsers.hasPrevious(),
        violatedUserRepository.countDistinctIps(start, end),
        violatedUserRepository.countDistinctEmails(start, end),
        topIps(start, end),
        topEmails(start, end));
  }

  private List<ViolatedUserIpSummary> topIps(LocalDateTime start, LocalDateTime end) {
    return violatedUserRepository.findTopIps(start, end, PageRequest.of(0, TOP_VIOLATION_LIMIT)).stream()
        .map(
            row ->
                new ViolatedUserIpSummary(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue(),
                    (LocalDateTime) row[3]))
        .toList();
  }

  private List<ViolatedUserEmailSummary> topEmails(LocalDateTime start, LocalDateTime end) {
    return violatedUserRepository
        .findTopEmails(start, end, PageRequest.of(0, TOP_VIOLATION_LIMIT))
        .stream()
        .map(
            row ->
                new ViolatedUserEmailSummary(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue(),
                    (LocalDateTime) row[3]))
        .toList();
  }

  private ViolatedUserInfo toViolatedUserInfo(
      ViolatedUser user, boolean includeIpInfo) {
    return new ViolatedUserInfo(
        user.getId(),
        user.getEmail(),
        user.getPassword(),
        user.getLoginLocation(),
        user.getIpAddress(),
        user.getLatitude(),
        user.getLongitude(),
        user.getLoggedInAt(),
        resolveIpInfo(user.getIpAddress(), includeIpInfo));
  }

  private ViolatedUserMapPoint toViolatedUserMapPoint(
      ViolatedUser user, boolean includeIpInfo) {
    return new ViolatedUserMapPoint(
        user.getId(),
        user.getEmail(),
        user.getLoginLocation(),
        user.getIpAddress(),
        user.getLatitude(),
        user.getLongitude(),
        user.getLoggedInAt(),
        resolveIpInfo(user.getIpAddress(), includeIpInfo));
  }

  private ViolatedUserIpInfo resolveIpInfo(String ipAddress, boolean includeIpInfo) {
    if (!includeIpInfo || ipAddress == null || ipAddress.isBlank()) {
      return null;
    }
    return ipInfoService.lookup(ipAddress.strip());
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
