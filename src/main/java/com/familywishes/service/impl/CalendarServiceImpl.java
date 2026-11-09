package com.familywishes.service.impl;

import com.familywishes.dto.CalendarDtos.CalendarResponse;
import com.familywishes.dto.EventDtos;
import com.familywishes.dto.FestivalDtos.FestivalResponse;
import com.familywishes.dto.UserDtos;
import com.familywishes.service.CalendarService;
import com.familywishes.service.EventService;
import com.familywishes.service.FestivalService;
import com.familywishes.service.UserService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

  @Override
  public List<CalendarResponse> listByMonth(
      Integer month, Integer year, boolean isAdmin, String userEmail) {
    List<FestivalResponse> festivals =
        year == null || year <= 0
            ? festivalService.listByMonth(month)
            : festivalService.listByMonthForYear(month, year);
    List<EventDtos.EventResponse> events =
        isAdmin
            ? eventService.listByMonth(month)
            : eventService.listByMonthAndUserEmail(month, userEmail);
    List<UserDtos.UserResponse> users =
        isAdmin
            ? userService.listByMonth(month)
            : userService.listByMonthAndUserEmail(month, userEmail);

    List<CalendarResponse> calendar = new ArrayList<>();
    festivals.forEach(festival -> calendar.add(toCalendar(festival, year)));
    events.forEach(event -> calendar.add(toCalendar(event)));
    users.forEach(user -> calendar.add(toCalendar(user)));
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
        null);
  }

  private CalendarResponse toCalendar(EventDtos.EventResponse event) {
    return new CalendarResponse(
        event.id(),
        "Anniversary",
        event.eventType(),
        event.eventDate(),
        event.active(),
        event.recurring(),
        event.userName());
  }

  /** Birthdays repeat every year. */
  private CalendarResponse toCalendar(UserDtos.UserResponse user) {
    return new CalendarResponse(
        user.id(), "Birthday", user.name(), user.dateOfBirth(), user.active(), true, user.name());
  }
}
