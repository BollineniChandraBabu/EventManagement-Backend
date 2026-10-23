package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familywishes.dto.EventDtos.EventRequest;
import com.familywishes.dto.EventDtos.EventResponse;
import com.familywishes.entity.Event;
import com.familywishes.entity.EventTypeSeed;
import com.familywishes.entity.User;
import com.familywishes.exception.BadRequestException;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.EventRepository;
import com.familywishes.repository.EventTypeSeedRepository;
import com.familywishes.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

  private static final String WISHES_URL = "https://example.com/anniversary/ravi.html";

  @Mock private EventRepository eventRepository;
  @Mock private UserRepository userRepository;
  @Mock private EventTypeSeedRepository eventTypeSeedRepository;

  @InjectMocks private EventServiceImpl eventService;

  private static User user(Long id, String email) {
    return User.builder().id(id).name("Ravi").email(email).build();
  }

  private static EventTypeSeed type(String code) {
    return EventTypeSeed.builder().code(code).displayName("Anniversary").build();
  }

  private static Event event(User owner, String wishesUrl) {
    return Event.builder()
        .id(5L)
        .eventType(type("ANNIVERSARY"))
        .eventDate(LocalDate.of(2018, 6, 10))
        .recurring(true)
        .wishesUrl(wishesUrl)
        .user(owner)
        .active(true)
        .build();
  }

  private void givenUserAndType(User owner) {
    when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
    when(eventTypeSeedRepository.findByCodeAndActiveTrue("ANNIVERSARY"))
        .thenReturn(Optional.of(type("ANNIVERSARY")));
  }

  @Test
  void createStoresTheCustomWishesLink() {
    User ravi = user(1L, "ravi@x.com");
    givenUserAndType(ravi);
    when(eventRepository.save(any(Event.class))).thenAnswer(call -> call.getArgument(0));

    EventResponse response =
        eventService.create(
            new EventRequest("anniversary", LocalDate.of(2018, 6, 10), true, 1L, " " + WISHES_URL + " "));

    assertEquals(WISHES_URL, response.wishesUrl());
    assertEquals(1L, response.userId());
  }

  @Test
  void createWithoutALinkLeavesTheLinkEmptySoTheEmailSkipsIt() {
    User ravi = user(1L, "ravi@x.com");
    givenUserAndType(ravi);
    when(eventRepository.save(any(Event.class))).thenAnswer(call -> call.getArgument(0));

    EventResponse response =
        eventService.create(new EventRequest("ANNIVERSARY", LocalDate.of(2018, 6, 10), true, 1L, "  "));

    assertNull(response.wishesUrl());
  }

  @Test
  void aLinkThatIsNotAWebAddressIsRejectedAndNothingIsSaved() {
    User ravi = user(1L, "ravi@x.com");
    givenUserAndType(ravi);

    assertThrows(
        BadRequestException.class,
        () ->
            eventService.create(
                new EventRequest("ANNIVERSARY", LocalDate.of(2018, 6, 10), true, 1L, "javascript:alert(1)")));
    verify(eventRepository, never()).save(any());
  }

  @Test
  void updateChangesAllFieldsAndAnEmptyLinkRemovesIt() {
    User ravi = user(1L, "ravi@x.com");
    Event existing = event(ravi, WISHES_URL);
    when(eventRepository.findById(5L)).thenReturn(Optional.of(existing));
    givenUserAndType(ravi);
    when(eventRepository.save(any(Event.class))).thenAnswer(call -> call.getArgument(0));

    EventResponse response =
        eventService.update(5L, new EventRequest("ANNIVERSARY", LocalDate.of(2019, 7, 1), false, 1L, ""));

    assertEquals(LocalDate.of(2019, 7, 1), response.eventDate());
    assertEquals(false, response.recurring());
    assertNull(response.wishesUrl());
  }

  @Test
  void updateOfAnUnknownEventIsNotFound() {
    when(eventRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> eventService.update(99L, new EventRequest("ANNIVERSARY", null, true, 1L, null)));
  }

  @Test
  void anEventCanBeReadByAnAdminOrItsOwnerButNotByOtherUsers() {
    Event existing = event(user(1L, "ravi@x.com"), WISHES_URL);
    when(eventRepository.findById(5L)).thenReturn(Optional.of(existing));

    assertEquals(5L, eventService.getById(5L, true, "admin@x.com").id());
    assertEquals(5L, eventService.getById(5L, false, "RAVI@x.com").id());
    assertThrows(NotFoundException.class, () -> eventService.getById(5L, false, "other@x.com"));
  }
}
