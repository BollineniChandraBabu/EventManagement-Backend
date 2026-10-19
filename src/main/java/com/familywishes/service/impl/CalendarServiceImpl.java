package com.familywishes.service.impl;

import com.familywishes.dto.CalendarDtos.CalendarResponse;
import com.familywishes.dto.EventDtos;
import com.familywishes.dto.FestivalDtos.FestivalResponse;
import com.familywishes.dto.UserDtos;
import com.familywishes.service.CalendarService;
import com.familywishes.service.EventService;
import com.familywishes.service.FestivalService;
import com.familywishes.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarServiceImpl implements CalendarService {

  private final FestivalService festivalService;
  private final EventService eventService;
  private final UserService userService;


  @Override
  public List<CalendarResponse> listByMonth(Integer month, boolean isAdmin, String userEmail) {
    List<FestivalResponse> festivals = festivalService.listByMonth(month);
    List<EventDtos.EventResponse> events =
        isAdmin
            ? eventService.listByMonth(month)
            : eventService.listByMonthAndUserEmail(month, userEmail);
    List<UserDtos.UserResponse> users =
        isAdmin
            ? userService.listByMonth(month)
            : userService.listByMonthAndUserEmail(month, userEmail);

    List<CalendarResponse> calendar = new ArrayList<>();
    festivals.forEach(festival -> calendar.add(toCalendar(festival)));
    events.forEach(event -> calendar.add(toCalendar(event)));
    users.forEach(user -> calendar.add(toCalendar(user)));
    return calendar;
  }

  /** Festival dates move every year, so they only count on their exact date. */
  private CalendarResponse toCalendar(FestivalResponse festival) {
    return new CalendarResponse(
        festival.id(),
        "Festival",
        festival.eventName(),
        festival.eventDate(),
        festival.active(),
        false,
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
