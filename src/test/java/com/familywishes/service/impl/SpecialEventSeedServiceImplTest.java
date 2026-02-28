package com.familywishes.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.familywishes.dto.CommonDtos.PagedResponse;
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
class SpecialEventSeedServiceImplTest {

  @Mock private SpecialEventRepository specialEventRepository;

  @InjectMocks private SpecialEventSeedServiceImpl specialEventSeedService;

  @Test
  void getByIdShouldThrowWhenMissing() {
    when(specialEventRepository.findById(99L)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> specialEventSeedService.getById(99L));
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
        specialEventSeedService.list(0, 10, "diw", "id", "desc");

    assertEquals(1, response.content().size());
    assertEquals("Diwali", response.content().get(0).eventName());
  }
}
