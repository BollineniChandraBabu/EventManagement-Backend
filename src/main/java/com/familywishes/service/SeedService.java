package com.familywishes.service;

import com.familywishes.dto.CommonDtos.PagedResponse;
import com.familywishes.dto.SeedDtos.SpecialEventSeedRequest;
import com.familywishes.dto.SeedDtos.SpecialEventSeedResponse;
import com.familywishes.dto.SeedDtos.WishTemplateSeedRequest;
import com.familywishes.dto.SeedDtos.WishTemplateSeedResponse;
import com.familywishes.entity.enums.SeedTemplateType;
import java.util.List;

public interface SeedService {
  SpecialEventSeedResponse createSpecialEventSeed(SpecialEventSeedRequest request);

  SpecialEventSeedResponse getSpecialEventSeedById(Long id);

  PagedResponse<SpecialEventSeedResponse> listSpecialEventSeeds(
      int page, int size, String searchKey, String sortBy, String sortDir);

  List<SpecialEventSeedResponse> listTodayActiveSpecialEventSeeds(int day, int month);

  SpecialEventSeedResponse updateSpecialEventSeed(Long id, SpecialEventSeedRequest request);

  void deleteSpecialEventSeed(Long id);

  WishTemplateSeedResponse createWishTemplateSeed(WishTemplateSeedRequest request);

  WishTemplateSeedResponse getWishTemplateSeedByType(SeedTemplateType type);

  List<WishTemplateSeedResponse> listWishTemplateSeeds();

  WishTemplateSeedResponse updateWishTemplateSeed(
      SeedTemplateType type, WishTemplateSeedRequest request);
}
