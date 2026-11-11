package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familywishes.dto.CalendarDtos.CalendarResponse;
import com.familywishes.dto.EventDtos.EventResponse;
import com.familywishes.dto.FestivalDtos.FestivalResponse;
import com.familywishes.dto.UserDtos.UserResponse;
import com.familywishes.service.CircleService;
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
  private static final Long ME_ID = 1L;

  @Mock private FestivalService festivalService;
  @Mock private EventService eventService;
  @Mock private UserService userService;
  @Mock private CircleService circleService;

  @InjectMocks private CalendarServiceImpl calendarService;

  private static UserResponse user(Long id, String name, LocalDate birthday) {
    return new UserResponse(
        id,
        name,
        name + "@x.com",
        null,
        null,
        birthday,
        true,
        null,
        false,
        false,
        false,
        false,
        null,
        null,
        null,
        false,
        true);
  }

  private static UserResponse birthdayOf(String name, LocalDate dateOfBirth) {
    return user(1L, name, dateOfBirth);
  }

  private static EventResponse event(
      Long id, String type, LocalDate date, boolean recurring, String owner, Long ownerId) {
    return new EventResponse(id, type, date, recurring, owner, true, ownerId, null, null);
  }

  /** The signed-in user and the ids of their accepted circle members. */
  private void signedInAs(Long id, Long... memberIds) {
    when(userService.getCurrentUser()).thenReturn(user(id, "Me", null));
    when(circleService.acceptedCircleMemberIds(id)).thenReturn(List.of(memberIds));
  }

  @Test
  void festivalsRecurEveryYearSoLaterYearsStillShowThem() {
    signedInAs(ME_ID);
    when(festivalService.listByMonth(NOVEMBER))
        .thenReturn(List.of(new FestivalResponse(5L, "Diwali", LocalDate.of(2026, 11, 8), true)));
    when(eventService.listByMonth(NOVEMBER)).thenReturn(List.of());
    when(userService.listByMonth(NOVEMBER)).thenReturn(List.of());

    CalendarResponse festival =
        calendarService.listByMonth(NOVEMBER, null, true, "admin@x.com").get(0);

    assertEquals("Festival", festival.eventType());
    assertTrue(festival.recurring());
    assertNull(festival.ownerName());
    assertNull(festival.ownerUserId());
    assertFalse(festival.fromCircle());
  }

  @Test
  void aRequestedYearUsesThatYearsFestivalDate() {
    signedInAs(ME_ID);
    when(festivalService.listByMonthForYear(NOVEMBER, 2027))
        .thenReturn(List.of(new FestivalResponse(5L, "Diwali", LocalDate.of(2027, 11, 1), true)));
    when(eventService.listByMonth(NOVEMBER)).thenReturn(List.of());
    when(userService.listByMonth(NOVEMBER)).thenReturn(List.of());

    CalendarResponse festival =
        calendarService.listByMonth(NOVEMBER, 2027, true, "admin@x.com").get(0);

    assertEquals(LocalDate.of(2027, 11, 1), festival.eventDate());
    assertFalse(festival.recurring());
    verify(festivalService).listByMonthForYear(NOVEMBER, 2027);
  }

  @Test
  void eventsKeepTheirRecurringFlagAndSayWhoTheyBelongTo() {
    signedInAs(ME_ID);
    when(festivalService.listByMonth(NOVEMBER)).thenReturn(List.of());
    when(eventService.listByMonth(NOVEMBER))
        .thenReturn(
            List.of(
                event(9L, "Wedding", LocalDate.of(2018, 11, 20), true, "Ravi", 3L),
                event(10L, "Housewarming", LocalDate.of(2026, 11, 2), false, "Meena", 4L)));
    when(userService.listByMonth(NOVEMBER)).thenReturn(List.of());

    List<CalendarResponse> calendar =
        calendarService.listByMonth(NOVEMBER, null, true, "admin@x.com");

    assertEquals("Anniversary", calendar.get(0).eventType());
    assertEquals("Wedding", calendar.get(0).eventName());
    assertTrue(calendar.get(0).recurring());
    assertEquals("Ravi", calendar.get(0).ownerName());
    assertEquals(3L, calendar.get(0).ownerUserId());
    assertFalse(calendar.get(1).recurring());
    assertEquals("Meena", calendar.get(1).ownerName());
  }

  @Test
  void birthdaysRepeatEveryYear() {
    signedInAs(ME_ID);
    when(festivalService.listByMonth(NOVEMBER)).thenReturn(List.of());
    when(eventService.listByMonth(NOVEMBER)).thenReturn(List.of());
    when(userService.listByMonth(NOVEMBER))
        .thenReturn(List.of(birthdayOf("Asha", LocalDate.of(1990, 11, 15))));

    CalendarResponse birthday =
        calendarService.listByMonth(NOVEMBER, null, true, "admin@x.com").get(0);

    assertEquals("Birthday", birthday.eventType());
    assertEquals("Asha", birthday.eventName());
    assertEquals(LocalDate.of(1990, 11, 15), birthday.eventDate());
    assertTrue(birthday.recurring());
    assertEquals("Asha", birthday.ownerName());
    assertEquals(1L, birthday.ownerUserId());
  }

  @Test
  void aUserWithoutCirclesOnlySeesTheirOwnEventsAndBirthday() {
    signedInAs(ME_ID);
    when(festivalService.listByMonth(NOVEMBER)).thenReturn(List.of());
    when(eventService.listByMonthAndUserEmail(NOVEMBER, "me@x.com")).thenReturn(List.of());
    when(userService.listByMonthAndUserEmail(NOVEMBER, "me@x.com")).thenReturn(List.of());

    calendarService.listByMonth(NOVEMBER, null, false, "me@x.com");

    verify(eventService).listByMonthAndUserEmail(NOVEMBER, "me@x.com");
    verify(userService).listByMonthAndUserEmail(NOVEMBER, "me@x.com");
    verify(eventService, never()).listByMonthForUsers(NOVEMBER, List.of(2L));
    verify(userService, never()).listByMonthForUsers(NOVEMBER, List.of(2L));
  }

  @Test
  void aUserSeesTheirCircleMembersEventsAndBirthdaysFlaggedAsFromCircle() {
    signedInAs(ME_ID, 2L);
    when(festivalService.listByMonth(NOVEMBER)).thenReturn(List.of());
    when(eventService.listByMonthAndUserEmail(NOVEMBER, "me@x.com"))
        .thenReturn(List.of(event(20L, "Wedding", LocalDate.of(2019, 11, 4), true, "Me", ME_ID)));
    when(userService.listByMonthAndUserEmail(NOVEMBER, "me@x.com"))
        .thenReturn(List.of(user(ME_ID, "Me", LocalDate.of(1990, 11, 2))));
    when(eventService.listByMonthForUsers(NOVEMBER, List.of(2L)))
        .thenReturn(
            List.of(event(21L, "Anniversary", LocalDate.of(2015, 11, 9), true, "Ravi", 2L)));
    when(userService.listByMonthForUsers(NOVEMBER, List.of(2L)))
        .thenReturn(List.of(user(2L, "Ravi", LocalDate.of(1988, 11, 25))));

    List<CalendarResponse> calendar =
        calendarService.listByMonth(NOVEMBER, null, false, "me@x.com");

    assertEquals(4, calendar.size());

    CalendarResponse ownEvent = byId(calendar, 20L);
    assertEquals("Me", ownEvent.ownerName());
    assertFalse(ownEvent.fromCircle());

    CalendarResponse friendEvent = byId(calendar, 21L);
    assertEquals("Ravi", friendEvent.ownerName());
    assertEquals(2L, friendEvent.ownerUserId());
    assertTrue(friendEvent.fromCircle());

    CalendarResponse ownBirthday = byId(calendar, ME_ID);
    assertEquals("Birthday", ownBirthday.eventType());
    assertEquals("Me", ownBirthday.ownerName());
    assertFalse(ownBirthday.fromCircle());

    CalendarResponse friendBirthday = byId(calendar, 2L);
    assertEquals("Birthday", friendBirthday.eventType());
    assertEquals("Ravi", friendBirthday.ownerName());
    assertTrue(friendBirthday.fromCircle());
  }

  private static CalendarResponse byId(List<CalendarResponse> calendar, Long id) {
    return calendar.stream()
        .filter(entry -> entry.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No calendar entry with id " + id));
  }

  @Test
  void anEntryIsNotDuplicatedWhenItAppearsInBothLists() {
    signedInAs(ME_ID, 2L);
    when(festivalService.listByMonth(NOVEMBER)).thenReturn(List.of());
    EventResponse shared = event(30L, "Wedding", LocalDate.of(2019, 11, 4), true, "Me", ME_ID);
    when(eventService.listByMonthAndUserEmail(NOVEMBER, "me@x.com")).thenReturn(List.of(shared));
    when(userService.listByMonthAndUserEmail(NOVEMBER, "me@x.com")).thenReturn(List.of());
    when(eventService.listByMonthForUsers(NOVEMBER, List.of(2L))).thenReturn(List.of(shared));
    when(userService.listByMonthForUsers(NOVEMBER, List.of(2L))).thenReturn(List.of());

    List<CalendarResponse> calendar =
        calendarService.listByMonth(NOVEMBER, null, false, "me@x.com");

    assertEquals(1, calendar.size());
    assertEquals(30L, calendar.get(0).id());
  }
}
