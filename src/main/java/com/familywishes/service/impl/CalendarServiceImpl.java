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
    List<CalendarResponse> calendarResponses = new ArrayList<>();
    List<FestivalResponse> festivalResponses = festivalService.listByMonth(month);
    List<EventDtos.EventResponse> eventResponses;
    List<UserDtos.UserResponse> userResponses;
    if(isAdmin) {
      eventResponses = eventService.listByMonth(month);
      userResponses = userService.listByMonth(month);
    }else {
      eventResponses = eventService.listByMonthAndUserEmail(month, userEmail);
      userResponses = userService.listByMonthAndUserEmail(month, userEmail);
    }
    calendarResponses.addAll(festivalResponses.stream().map(festivalResponse -> new CalendarResponse(festivalResponse.id(),festivalResponse.eventName(),festivalResponse.eventDate(),festivalResponse.active())).toList());
    calendarResponses.addAll(eventResponses.stream().map(eventResponse -> new CalendarResponse(eventResponse.id(),eventResponse.eventType(),eventResponse.eventDate(),eventResponse.active())).toList());
    calendarResponses.addAll(userResponses.stream().map(userResponse -> new CalendarResponse(userResponse.id(), "Birthday", userResponse.dateOfBirth(),userResponse.active())).toList());
    return calendarResponses;
  }
}