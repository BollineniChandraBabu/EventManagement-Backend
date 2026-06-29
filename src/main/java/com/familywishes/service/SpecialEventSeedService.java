package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.SpecialEventSeedRequest;
import com.familywishes.dto.SeedDtos.SpecialEventSeedResponse;
import java.time.LocalDate;

public interface SpecialEventSeedService {
  SpecialEventSeedResponse create(SpecialEventSeedRequest request);

  SpecialEventSeedResponse getById(Long id);

  PagedResponse<SpecialEventSeedResponse> list(
      int page, int size, String searchKey, String sortBy, String sortDir);

  PagedResponse<SpecialEventSeedResponse> listTodayActive(
      LocalDate eventDate, int page, int size, String searchKey, String sortBy, String sortDir);

  SpecialEventSeedResponse update(Long id, SpecialEventSeedRequest request);

  void delete(Long id);
}
