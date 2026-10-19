package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.familywishes.dto.CalendarDtos.CalendarResponse;
import com.familywishes.dto.EventDtos.EventResponse;
import com.familywishes.dto.FestivalDtos.FestivalResponse;
import com.familywishes.dto.UserDtos.UserResponse;
import com.familywishes.service.EventService;
import com.familywishes.service.FestivalService;
import com.familywishes.service.UserService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarServiceImplTest {

  private static final int NOVEMBER = 11;

  @Mock private FestivalService festivalService;
  @Mock private EventService eventService;
  @Mock private UserService userService;

  @InjectMocks private CalendarServiceImpl calendarService;

  private static UserResponse birthdayOf(String name, LocalDate dateOfBirth) {
    return new UserResponse(
        1L, name, name + "@x.com", null, null, dateOfBirth, true, null, false, false, false, false,
        null, null);
  }

  @Test
  void festivalsOnlyCountOnTheirExactDate() {
    when(festivalService.listByMonth(NOVEMBER))
        .thenReturn(List.of(new FestivalResponse(5L, "Diwali", LocalDate.of(2026, 11, 8), true)));
    when(eventService.listByMonth(NOVEMBER)).thenReturn(List.of());
    when(userService.listByMonth(NOVEMBER)).thenReturn(List.of());

    CalendarResponse festival = calendarService.listByMonth(NOVEMBER, true, "admin@x.com").get(0);

    assertEquals("Festival", festival.eventType());
    assertFalse(festival.recurring());
    assertNull(festival.ownerName());
  }

  @Test
  void eventsKeepTheirRecurringFlagAndSayWhoTheyBelongTo() {
    when(festivalService.listByMonth(NOVEMBER)).thenReturn(List.of());
    when(eventService.listByMonth(NOVEMBER))
        .thenReturn(
            List.of(
                new EventResponse(9L, "Wedding", LocalDate.of(2018, 11, 20), true, "Ravi", true),
                new EventResponse(10L, "Housewarming", LocalDate.of(2026, 11, 2), false, "Meena", true)));
    when(userService.listByMonth(NOVEMBER)).thenReturn(List.of());

    List<CalendarResponse> calendar = calendarService.listByMonth(NOVEMBER, true, "admin@x.com");

    assertEquals("Anniversary", calendar.get(0).eventType());
    assertEquals("Wedding", calendar.get(0).eventName());
    assertTrue(calendar.get(0).recurring());
    assertEquals("Ravi", calendar.get(0).ownerName());
    assertFalse(calendar.get(1).recurring());
    assertEquals("Meena", calendar.get(1).ownerName());
  }

  @Test
  void birthdaysRepeatEveryYear() {
    when(festivalService.listByMonth(NOVEMBER)).thenReturn(List.of());
    when(eventService.listByMonth(NOVEMBER)).thenReturn(List.of());
    when(userService.listByMonth(NOVEMBER))
        .thenReturn(List.of(birthdayOf("Asha", LocalDate.of(1990, 11, 15))));

    CalendarResponse birthday = calendarService.listByMonth(NOVEMBER, true, "admin@x.com").get(0);

    assertEquals("Birthday", birthday.eventType());
    assertEquals("Asha", birthday.eventName());
    assertEquals(LocalDate.of(1990, 11, 15), birthday.eventDate());
    assertTrue(birthday.recurring());
    assertEquals("Asha", birthday.ownerName());
  }

  @Test
  void regularUsersOnlySeeTheirOwnEventsAndBirthday() {
    when(festivalService.listByMonth(NOVEMBER)).thenReturn(List.of());
    when(eventService.listByMonthAndUserEmail(NOVEMBER, "me@x.com")).thenReturn(List.of());
    when(userService.listByMonthAndUserEmail(NOVEMBER, "me@x.com")).thenReturn(List.of());

    calendarService.listByMonth(NOVEMBER, false, "me@x.com");

    verify(eventService).listByMonthAndUserEmail(NOVEMBER, "me@x.com");
    verify(userService).listByMonthAndUserEmail(NOVEMBER, "me@x.com");
    verifyNoMoreInteractions(eventService, userService);
  }
}
