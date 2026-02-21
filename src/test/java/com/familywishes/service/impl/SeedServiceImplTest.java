package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.SpecialEventSeedRequest;
import com.familywishes.dto.SeedDtos.SpecialEventSeedResponse;
import com.familywishes.entity.SpecialEvent;
import com.familywishes.exception.NotFoundException;
import com.familywishes.repository.SpecialEventRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SeedServiceImplTest {

  @Mock private SpecialEventRepository specialEventRepository;

  @InjectMocks private SeedServiceImpl seedService;

  @Test
  void getByIdShouldThrowWhenMissing() {
    when(specialEventRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> seedService.getSpecialEventSeedById(99L));
  }

  @Test
  void listShouldReturnPagedResponse() {
    SpecialEvent event =
        SpecialEvent.builder()
            .id(1L)
            .eventName("Diwali")
            .day(1)
            .month(11)
            .message("Happy Diwali")
            .active(true)
            .build();

    when(specialEventRepository.findAllBySearchKey(any(), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(java.util.List.of(event), PageRequest.of(0, 10), 1));

    PagedResponse<SpecialEventSeedResponse> response =
        seedService.listSpecialEventSeeds(0, 10, "diw", "id", "desc");

    assertEquals(1, response.content().size());
    assertEquals("Diwali", response.content().get(0).eventName());
  }

  @Test
  void listTodayShouldReturnOnlyActiveRowsForGivenDayMonth() {
    SpecialEvent event =
        SpecialEvent.builder()
            .id(4L)
            .eventName("Ugadi")
            .day(9)
            .month(4)
            .message("Happy Ugadi")
            .active(true)
            .build();

    when(specialEventRepository.findByDayAndMonthAndActiveTrue(9, 4))
        .thenReturn(java.util.List.of(event));

    var response = seedService.listTodayActiveSpecialEventSeeds(9, 4);

    assertEquals(1, response.size());
    assertEquals("Ugadi", response.get(0).eventName());
  }

  @Test
  void updateShouldPersistChanges() {
    SpecialEvent existing =
        SpecialEvent.builder()
            .id(1L)
            .eventName("Old")
            .day(1)
            .month(1)
            .message("Old message")
            .active(true)
            .build();

    when(specialEventRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(specialEventRepository.save(any(SpecialEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    SpecialEventSeedResponse response =
        seedService.updateSpecialEventSeed(
            1L, new SpecialEventSeedRequest("New", 2, 2, "New message", false));

    assertEquals("New", response.eventName());
    assertEquals(2, response.day());
    assertFalse(response.active());
  }
}
