package com.familywishes.service.impl;

import com.familywishes.dto.CalendarDtos.CalendarResponse;
import com.familywishes.dto.EventDtos;
import com.familywishes.dto.FestivalDtos.FestivalResponse;
import com.familywishes.dto.UserDtos;
import com.familywishes.service.CalendarService;
import com.familywishes.service.CircleService;
import com.familywishes.service.EventService;
import com.familywishes.service.FestivalService;
import com.familywishes.service.UserService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarServiceImpl implements CalendarService {

  private final FestivalService festivalService;
  private final EventService eventService;
  private final UserService userService;
  private final CircleService circleService;

  @Override
  public List<CalendarResponse> listByMonth(
      Integer month, Integer year, boolean isAdmin, String userEmail) {
    List<FestivalResponse> festivals =
        year == null || year <= 0
            ? festivalService.listByMonth(month)
            : festivalService.listByMonthForYear(month, year);

    Long meId = userService.getCurrentUser().id();
    Set<Long> circleMemberIds = Set.copyOf(circleService.acceptedCircleMemberIds(meId));

    List<EventDtos.EventResponse> events;
    List<UserDtos.UserResponse> users;
    if (isAdmin) {
      // Admins already see everyone; circle entries are simply flagged as such.
      events = eventService.listByMonth(month);
      users = userService.listByMonth(month);
    } else {
      List<Long> memberIds = new ArrayList<>(circleMemberIds);
      if (memberIds.isEmpty()) {
        events = eventService.listByMonthAndUserEmail(month, userEmail);
        users = userService.listByMonthAndUserEmail(month, userEmail);
      } else {
        // A user sees their own events and birthdays, plus those of their circle members.
        events =
            mergeById(
                eventService.listByMonthAndUserEmail(month, userEmail),
                eventService.listByMonthForUsers(month, memberIds),
                EventDtos.EventResponse::id);
        users =
            mergeById(
                userService.listByMonthAndUserEmail(month, userEmail),
                userService.listByMonthForUsers(month, memberIds),
                UserDtos.UserResponse::id);
      }
    }

    List<CalendarResponse> calendar = new ArrayList<>();
    festivals.forEach(festival -> calendar.add(toCalendar(festival, year)));
    events.forEach(event -> calendar.add(toCalendar(event, circleMemberIds)));
    users.forEach(user -> calendar.add(toCalendar(user, circleMemberIds)));
    return calendar;
  }

  /**
   * Festivals are stored per year. When the calendar is asked for a specific year the entry keeps
   * that year's date and is marked as not recurring, so navigating to another year shows that
   * year's own festivals (fetched on demand by the festival service) instead of repeating one
   * year's dates forever. Without a year the older month-only behaviour is kept: the festival
   * repeats yearly on its month/day.
   */
  private CalendarResponse toCalendar(FestivalResponse festival, Integer year) {
    boolean yearScoped = year != null && year > 0;
    LocalDate eventDate = festival.eventDate();
    if (eventDate != null && yearScoped) {
      eventDate = eventDate.withYear(year);
    }
    return new CalendarResponse(
        festival.id(),
        "Festival",
        festival.eventName(),
        eventDate,
        festival.active(),
        !yearScoped,
        null,
        null,
        false);
  }

  private CalendarResponse toCalendar(EventDtos.EventResponse event, Set<Long> circleMemberIds) {
    return new CalendarResponse(
        event.id(),
        "Anniversary",
        event.eventType(),
        event.eventDate(),
        event.active(),
        event.recurring(),
        event.userName(),
        event.userId(),
        isFromCircle(event.userId(), circleMemberIds));
  }

  /** Birthdays repeat every year. */
  private CalendarResponse toCalendar(UserDtos.UserResponse user, Set<Long> circleMemberIds) {
    return new CalendarResponse(
        user.id(),
        "Birthday",
        user.name(),
        user.dateOfBirth(),
        user.active(),
        true,
        user.name(),
        user.id(),
        isFromCircle(user.id(), circleMemberIds));
  }

  private boolean isFromCircle(Long ownerUserId, Set<Long> circleMemberIds) {
    return ownerUserId != null && circleMemberIds.contains(ownerUserId);
  }

  /** Keeps the first entry for each id, so own entries win over a duplicate circle entry. */
  private static <T> List<T> mergeById(List<T> own, List<T> fromCircle, Function<T, Long> idOf) {
    LinkedHashMap<Long, T> byId = new LinkedHashMap<>();
    own.forEach(entry -> byId.putIfAbsent(idOf.apply(entry), entry));
    fromCircle.forEach(entry -> byId.putIfAbsent(idOf.apply(entry), entry));
    return new ArrayList<>(byId.values());
  }
}
