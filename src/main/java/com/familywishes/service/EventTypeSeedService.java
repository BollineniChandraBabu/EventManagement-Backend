package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import java.util.List;

public interface EventTypeSeedService {
  EnumSeedResponse create(EnumSeedRequest request);

  EnumSeedResponse update(String code, EnumSeedRequest request);

  List<EnumSeedResponse> listActive();

  PagedResponse<EnumSeedResponse> list(
      int page, int size, String searchKey, String sortBy, String sortDir);

  EnumSeedResponse getById(Long id);

  void deleteById(Long id);
}
