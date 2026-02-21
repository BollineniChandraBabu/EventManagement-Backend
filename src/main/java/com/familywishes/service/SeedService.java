package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.SpecialEventSeedRequest;
import com.familywishes.dto.SeedDtos.SpecialEventSeedResponse;
import java.util.List;

public interface SeedService {
  SpecialEventSeedResponse createSpecialEventSeed(SpecialEventSeedRequest request);

  SpecialEventSeedResponse getSpecialEventSeedById(Long id);

  PagedResponse<SpecialEventSeedResponse> listSpecialEventSeeds(
      int page, int size, String searchKey, String sortBy, String sortDir);

  List<SpecialEventSeedResponse> listTodayActiveSpecialEventSeeds(int day, int month);

  SpecialEventSeedResponse updateSpecialEventSeed(Long id, SpecialEventSeedRequest request);

  void deleteSpecialEventSeed(Long id);
}
