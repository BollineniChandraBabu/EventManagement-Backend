package com.familywishes.service;

import com.familywishes.dto.SeedDtos.EnumSeedRequest;
import com.familywishes.dto.SeedDtos.EnumSeedResponse;
import java.util.List;

public interface RelationshipSeedService {
  EnumSeedResponse create(EnumSeedRequest request);

  EnumSeedResponse update(String code, EnumSeedRequest request);

  List<EnumSeedResponse> listActive();

  EnumSeedResponse getById(Long id);
}
