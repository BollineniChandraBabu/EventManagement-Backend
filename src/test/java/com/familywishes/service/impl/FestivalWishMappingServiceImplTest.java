package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.familywishes.dto.FestivalDtos.FestivalWishMappingBulkRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingBulkResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingRequest;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingResponse;
import com.familywishes.dto.FestivalDtos.FestivalWishMappingSummary;
import com.familywishes.entity.FestivalWishMapping;
import com.familywishes.entity.SpecialEvent;
import com.familywishes.entity.User;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.FestivalWishMappingRepository;
import com.familywishes.repository.SpecialEventRepository;
import com.familywishes.repository.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FestivalWishMappingServiceImplTest {

  private static final Long FESTIVAL_ID = 5L;

  @Mock private FestivalWishMappingRepository mappingRepository;
  @Mock private SpecialEventRepository specialEventRepository;
  @Mock private UserRepository userRepository;

  @InjectMocks private FestivalWishMappingServiceImpl service;

  private final SpecialEvent diwali =
      SpecialEvent.builder().id(FESTIVAL_ID).eventName("Diwali").eventDate(LocalDate.now()).build();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "schedulerTimeZone", "UTC");
  }

  private static User user(Long id) {
    return User.builder().id(id).name("User" + id).build();
  }

  private void givenFestival() {
    when(specialEventRepository.findById(FESTIVAL_ID)).thenReturn(Optional.of(diwali));
  }

  @Test
  void upsertKeepsATrimmedPersonalNoteAndBlankMeansNoNote() {
    givenFestival();
    when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
    when(mappingRepository.findBySpecialEvent_IdAndUser_Id(FESTIVAL_ID, 1L)).thenReturn(Optional.empty());
    when(mappingRepository.save(any(FestivalWishMapping.class))).thenAnswer(call -> call.getArgument(0));

    FestivalWishMappingResponse withNote =
        service.upsert(new FestivalWishMappingRequest(FESTIVAL_ID, 1L, true, "  Happy Diwali!  "));
    FestivalWishMappingResponse blank =
        service.upsert(new FestivalWishMappingRequest(FESTIVAL_ID, 1L, true, "   "));

    assertEquals("Happy Diwali!", withNote.customMessage());
    assertNull(blank.customMessage());
  }

  @Test
  void bulkCreatesMissingMappingsAndUpdatesExistingOnesInPlace() {
    givenFestival();
    FestivalWishMapping existing = new FestivalWishMapping();
    existing.setSpecialEvent(diwali);
    existing.setUser(user(2L));
    existing.setActive(false);
    when(userRepository.findAllById(List.of(1L, 2L, 3L)))
        .thenReturn(List.of(user(1L), user(2L), user(3L)));
    when(mappingRepository.findBySpecialEvent_Id(FESTIVAL_ID)).thenReturn(List.of(existing));

    FestivalWishMappingBulkResponse result =
        service.upsertBulk(
            new FestivalWishMappingBulkRequest(FESTIVAL_ID, List.of(1L, 2L, 3L, 2L, 1L), true, " Dear all "));

    assertEquals(2, result.created());
    assertEquals(1, result.updated());
    assertTrue(existing.isActive());
    assertEquals("Dear all", existing.getCustomMessage());
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<FestivalWishMapping>> saved = ArgumentCaptor.forClass(Iterable.class);
    verify(mappingRepository).saveAll(saved.capture());
    assertEquals(3, ((List<?>) saved.getValue()).size());
  }

  @Test
  void bulkWithAnUnknownUserFailsBeforeAnythingIsSaved() {
    givenFestival();
    when(userRepository.findAllById(List.of(1L, 99L))).thenReturn(List.of(user(1L)));

    assertThrows(
        NotFoundException.class,
        () -> service.upsertBulk(new FestivalWishMappingBulkRequest(FESTIVAL_ID, List.of(1L, 99L), true, null)));
  }

  @Test
  void bulkWithAnUnknownFestivalFails() {
    when(specialEventRepository.findById(404L)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> service.upsertBulk(new FestivalWishMappingBulkRequest(404L, List.of(1L), true, null)));
  }

  @Test
  void summaryCountsUpcomingMappingsOverTheNext30Days() {
    LocalDate today = LocalDate.now(ZoneId.of("UTC"));
    when(mappingRepository.count()).thenReturn(10L);
    when(mappingRepository.countByActiveTrue()).thenReturn(7L);
    when(mappingRepository.countByIsError(true)).thenReturn(2L);
    when(mappingRepository.countByActiveTrueAndSpecialEvent_EventDateBetween(today, today.plusDays(30)))
        .thenReturn(3L);

    FestivalWishMappingSummary summary = service.summary();

    assertEquals(new FestivalWishMappingSummary(10, 7, 3, 3, 2), summary);
  }

  @Test
  void listPassesATrimmedSearchAndOnlyKnownStatuses() {
    when(mappingRepository.findAllBySearchKey(any(), any(), eq(true), any(Pageable.class)))
        .thenReturn(Page.empty());

    service.list(0, 10, "  diwali ", "failed", "id", "desc");
    service.list(0, 10, "", "drop table", "id", "desc");
    service.list(0, 10, "", null, "id", "desc");

    verify(mappingRepository).findAllBySearchKey(eq("diwali"), eq("FAILED"), eq(true), any(Pageable.class));
    verify(mappingRepository, org.mockito.Mockito.times(2))
        .findAllBySearchKey(eq(""), eq("ALL"), eq(true), any(Pageable.class));
  }
}
